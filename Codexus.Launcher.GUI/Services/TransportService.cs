using System.Collections.ObjectModel;
using Avalonia.Threading;
using Codexus.ModHost.Event;
using Codexus.OpenSDK.Entities.Yggdrasil;
using Codexus.OpenSDK.Yggdrasil;
using Codexus.OpenTransport.Entities.Transport;
using Codexus.OpenTransport.Event;
using Serilog;

namespace Codexus.Launcher.GUI.Services;

/// <summary>
/// 对 OpenTransport 的一层 UI 友好包装：保留当前实例 + 当前状态，
/// 把 EventBus 上的事件路由到 UI 友好回调。注意 EventBus 是全局静态，
/// 多次 launch 会复用同一订阅，这里只用一份订阅（构造时订阅）。
/// </summary>
public sealed class TransportService : IDisposable
{
    private readonly ILogger _logger;
    private readonly UiLogSink _log;
    private Codexus.OpenTransport.OpenTransport? _current;
    private StandardYggdrasil? _yggdrasil;

    public TransportState State { get; private set; } = TransportState.Idle;
    public event EventHandler<TransportState>? StateChanged;

    public TransportService(ILogger logger, UiLogSink log)
    {
        _logger = logger;
        _log = log;

        EventBus.Instance.Subscribe<EventJoinServer>(async e =>
        {
            try
            {
                if (_yggdrasil is null)
                {
                    _log.Post("WRN", "Join server event fired but no yggdrasil configured, skipping.");
                    return;
                }
                var join = await _yggdrasil.JoinServerAsync(e.Context.Session.Profile, e.ServerId);
                if (join.IsSuccess) _log.Post("INF", "Joined server successfully.");
                else _log.Post("ERR", "Join server failed: " + join.Error);
            }
            catch (Exception ex)
            {
                _log.Post("ERR", "Join server handler exception", ex);
            }
        });
    }

    public void Start(
        SavedAccount account,
        ServerEntry server)
    {
        if (State == TransportState.Connecting || State == TransportState.Connected)
        {
            _log.Post("WRN", "Transport already running, ignoring start request.");
            return;
        }
        if (string.IsNullOrEmpty(account.EntityId) || string.IsNullOrEmpty(account.Token))
        {
            _log.Post("ERR", "Account has no EntityId/Token, cannot launch. Need a successful X19Resume first.");
            return;
        }

        SetState(TransportState.Connecting);
        _ = Task.Run(async () =>
        {
            try
            {
                var profile = new GameProfile
                {
                    GameId = server.GameId,
                    GameVersion = server.GameVersion,
                    BootstrapMd5 = server.BootstrapMd5,
                    DatFileMd5 = server.DatMd5,
                    Mods = new ModList(),
                    User = new UserProfile
                    {
                        UserId = int.Parse(account.EntityId),
                        UserToken = account.Token
                    }
                };
                var request = new CreateRequest
                {
                    ServerAddress = server.Address,
                    ServerPort = server.Port,
                    RoleName = server.RoleName,
                    Debug = false
                };
                _yggdrasil = new StandardYggdrasil(new YggdrasilData
                {
                    LauncherVersion = server.GameVersion,
                    Channel = "netease",
                    CrcSalt = server.CrcSalt
                });
                _current = Codexus.OpenTransport.OpenTransport.Create(profile, request, _logger);
                await _current.StartAsync();
                SetState(TransportState.Connected);
            }
            catch (Exception ex)
            {
                _log.Post("ERR", "Transport start failed", ex);
                SetState(TransportState.Failed);
            }
        });
    }

    public void Stop()
    {
        if (_current is null) return;
        _ = Task.Run(async () =>
        {
            try
            {
                await _current.StopAsync();
                _log.Post("INF", "Transport stopped.");
            }
            catch (Exception ex)
            {
                _log.Post("ERR", "Transport stop failed", ex);
            }
            finally
            {
                SetState(TransportState.Idle);
            }
        });
    }

    private void SetState(TransportState state)
    {
        State = state;
        StateChanged?.Invoke(this, state);
    }

    public void Dispose()
    {
        Stop();
    }
}

public enum TransportState { Idle, Connecting, Connected, Failed }
