using Codexus.OpenSDK.Entities.MPay;
using Codexus.OpenSDK.Entities.X19;
using Codexus.OpenSDK.Exceptions;

namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 网易 X19 邮箱密码通道适配器。
/// 流程：InitializeDeviceAsync → LoginWithEmailAsync → X19.ContinueAsync(user, device)。
/// 当邮箱密码触发风控（VerifyException, code 1351）时返回 NeedsSmsVerify，
/// 调用方需改走 X19Sms 通道（同账号、附 SmsCode）完成 SMS 二次验证。
/// </summary>
internal sealed class X19EmailAuthenticator : IAuthenticator
{
    private readonly X19 _x19 = new();

    public AuthChannel Channel => AuthChannel.X19Email;

    public async Task<AuthResult> AuthenticateAsync(AuthRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);
        if (string.IsNullOrWhiteSpace(request.Password))
            throw new ArgumentException("Password is required for X19Email channel.", nameof(request));

        cancellationToken.ThrowIfCancellationRequested();
        try
        {
            await _x19.InitializeDeviceAsync();
            var user = await _x19.LoginWithEmailAsync(request.Username, request.Password!);
            if (user == null)
            {
                return new AuthResult
                {
                    Channel = Channel,
                    Status = AuthStatus.Failed,
                    Account = request.Username,
                    Error = "Email login returned no user payload."
                };
            }

            var device = _x19.Device ?? await _x19.InitializeDeviceAsync();
            var (otp, loginChannel) = await _x19.ContinueAsync(user, device);

            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.Success,
                Account = otp.Account,
                EntityId = otp.EntityId,
                Token = otp.Token,
                SAuthJson = otp.UniSdkLoginJson
            };
        }
        catch (VerifyException ex)
        {
            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.NeedsSmsVerify,
                Account = request.Username,
                Error = ex.Message
            };
        }
        catch (Exception ex)
        {
            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.Failed,
                Account = request.Username,
                Error = ex.Message
            };
        }
    }

    public void Dispose()
    {
        _x19.Dispose();
        GC.SuppressFinalize(this);
    }

    public ValueTask DisposeAsync()
    {
        _x19.Dispose();
        return ValueTask.CompletedTask;
    }
}
