using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Codexus.OpenSDK.Authentication;
using Codexus.Launcher.GUI.Services;
using Serilog;

namespace Codexus.Launcher.GUI.ViewModels.Pages;

public partial class AuthPageViewModel : ViewModelBase
{
    private readonly AuthManager _auth;
    private readonly LauncherStateService _state;
    private readonly TransportService _transport;
    private readonly ILogger _log;

    public ObservableCollection<string> ChannelOptions { get; } = new()
    {
        "C4399", "X19Email", "X19Sms", "X19Resume"
    };

    // 服务器列表共享 LauncherStateService.Servers，避免双向同步
    public ObservableCollection<ServerEntry> Servers => _state.Servers;

    [ObservableProperty] private string _channel = "C4399";
    [ObservableProperty] private string? _username;
    [ObservableProperty] private string? _password;
    [ObservableProperty] private string? _captcha;
    [ObservableProperty] private string? _captchaSession;
    [ObservableProperty] private string? _smsCode;
    [ObservableProperty] private string? _smsTicket;
    [ObservableProperty] private string? _sauthJson;
    [ObservableProperty] private string? _lastEntityId;
    [ObservableProperty] private string? _lastToken;
    [ObservableProperty] private string? _lastSAuthJson;
    [ObservableProperty] private string? _statusText;
    [ObservableProperty] private string? _errorText;
    [ObservableProperty] private bool _isBusy;
    [ObservableProperty] private ServerEntry? _selectedServer;

    public AuthPageViewModel(AuthManager auth, LauncherStateService state,
        TransportService transport, ILogger log)
    {
        _auth = auth;
        _state = state;
        _transport = transport;
        _log = log;
        _statusText = "未登录";
        if (Servers.Count > 0) _selectedServer = Servers[0];
    }

    partial void OnChannelChanged(string value)
    {
        // 切渠道时清空上次结果，避免用户误用
        ErrorText = null;
        StatusText = "未登录";
        LastEntityId = null;
        LastToken = null;
        LastSAuthJson = null;
    }

    [RelayCommand]
    private async Task LoginAsync()
    {
        if (IsBusy) return;
        IsBusy = true;
        ErrorText = null;
        StatusText = "登录中...";

        try
        {
            var channel = Enum.Parse<AuthChannel>(Channel);
            var request = BuildRequest(channel);
            var result = await _auth.AuthenticateAsync(channel, request);

            // C4399 成功后需要链式走 X19Resume 拿完整凭证（沿用 ExampleConsole 的做法）
            if (result.IsSuccess && channel == AuthChannel.C4399
                && !string.IsNullOrEmpty(result.SAuthJson))
            {
                _log.Information("C4399 OK, chaining X19Resume...");
                result = await _auth.AuthenticateAsync(AuthChannel.X19Resume, new AuthRequest
                {
                    Username = Username ?? "c4399-resume",
                    SAuthJson = result.SAuthJson
                });
            }

            ApplyResult(result);
            if (result.IsSuccess)
            {
                _state.SaveAccount(result);
            }
        }
        catch (ArgumentException ex)
        {
            ErrorText = "参数错误: " + ex.Message;
            StatusText = "失败";
        }
        catch (Exception ex)
        {
            ErrorText = ex.Message;
            StatusText = "异常";
            _log.Error(ex, "Login failed");
        }
        finally
        {
            IsBusy = false;
        }
    }

    [RelayCommand]
    private void SaveLastSAuth()
    {
        if (string.IsNullOrEmpty(LastSAuthJson)) return;
        try
        {
            var path = Path.Combine(AppContext.BaseDirectory, "saved_sauth.json");
            File.WriteAllText(path, LastSAuthJson);
            StatusText = $"SAuth 已保存到 {Path.GetFileName(path)}";
        }
        catch (Exception ex)
        {
            ErrorText = "保存失败: " + ex.Message;
        }
    }

    /// <summary>
    /// 启动 OpenTransport 连接：选当前服务器 + 上次成功的账号。
    /// 账号优先取刚登录的 LastEntityId/LastToken，否则回退到本地已存的同渠道最近账号。
    /// </summary>
    [RelayCommand]
    private void StartConnect()
    {
        if (SelectedServer is null)
        {
            ErrorText = "请先选择服务器";
            return;
        }

        var account = BuildCurrentAccount();
        if (account is null || string.IsNullOrEmpty(account.EntityId) || string.IsNullOrEmpty(account.Token))
        {
            ErrorText = "当前没有可用账号：请先登录成功（EntityId/Token 必须有值）";
            return;
        }

        ErrorText = null;
        StatusText = $"启动连接：{SelectedServer.Name} ({SelectedServer.Address}:{SelectedServer.Port})";
        try
        {
            _transport.Start(account, SelectedServer);
            StatusText = "连接已发起，切到控制台页查看日志";
        }
        catch (Exception ex)
        {
            ErrorText = "启动失败: " + ex.Message;
            StatusText = "失败";
            _log.Error(ex, "StartConnect failed");
        }
    }

    /// <summary>
    /// 构造当前可用账号：优先用本次登录页上展示的 LastEntityId/LastToken；
    /// 否则从本地 SavedAccount 中按当前渠道取最近一条。
    /// </summary>
    private SavedAccount? BuildCurrentAccount()
    {
        if (!string.IsNullOrEmpty(LastEntityId) && !string.IsNullOrEmpty(LastToken))
        {
            var channel = Enum.Parse<AuthChannel>(Channel);
            return new SavedAccount
            {
                Channel = channel,
                Account = Username ?? LastEntityId,
                EntityId = LastEntityId,
                Token = LastToken,
                SAuthJson = LastSAuthJson ?? "",
                SavedAt = DateTime.Now
            };
        }

        var currentChannel = Enum.Parse<AuthChannel>(Channel);
        return _state.Accounts.LastOrDefault(a => a.Channel == currentChannel);
    }

    private AuthRequest BuildRequest(AuthChannel channel)
    {
        return channel switch
        {
            AuthChannel.C4399 => new AuthRequest
            {
                Username = Username ?? throw new ArgumentException("Username 必填"),
                Password = Password,
                Captcha = Captcha,
                CaptchaSessionId = CaptchaSession
            },
            AuthChannel.X19Email => new AuthRequest
            {
                Username = Username ?? throw new ArgumentException("Username (email) 必填"),
                Password = Password
            },
            AuthChannel.X19Sms => new AuthRequest
            {
                Username = Username ?? throw new ArgumentException("Username (phone) 必填"),
                SmsCode = SmsCode,
                SmsTicket = SmsTicket
            },
            AuthChannel.X19Resume => new AuthRequest
            {
                Username = Username ?? "x19-resume",
                SAuthJson = SauthJson ?? throw new ArgumentException("SAuthJson 必填")
            },
            _ => throw new ArgumentOutOfRangeException(nameof(channel))
        };
    }

    private void ApplyResult(AuthResult r)
    {
        StatusText = r.Status.ToString();
        LastEntityId = r.EntityId;
        LastToken = r.Token;
        LastSAuthJson = r.SAuthJson;
        ErrorText = r.Error;

        if (r.Status == AuthStatus.NeedsCaptcha)
            ErrorText = "需要验证码。填写 Captcha + CaptchaSessionId 后重新登录。";
        if (r.Status == AuthStatus.NeedsSmsVerify)
            ErrorText = $"需要短信验证。SmsTicket={r.SmsTicket}。填写短信码后重新登录。";
    }
}
