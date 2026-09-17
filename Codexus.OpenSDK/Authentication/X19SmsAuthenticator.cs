using Codexus.OpenSDK.Entities.MPay;
using Codexus.OpenSDK.Exceptions;

namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 网易 X19 短信通道适配器。两段式：
///   1) 仅带 Username（手机号）且不带 SmsCode：发短信，返回 NeedsSmsVerify + SmsTicket。
///   2) 带 Username + SmsCode + SmsTicket：verify_sms → finish → X19.ContinueAsync(user, device)。
/// </summary>
internal sealed class X19SmsAuthenticator : IAuthenticator
{
    private readonly X19 _x19 = new();

    public AuthChannel Channel => AuthChannel.X19Sms;

    public async Task<AuthResult> AuthenticateAsync(AuthRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);
        if (string.IsNullOrWhiteSpace(request.Username))
            throw new ArgumentException("Username (phone number) is required for X19Sms channel.", nameof(request));

        cancellationToken.ThrowIfCancellationRequested();
        try
        {
            await _x19.InitializeDeviceAsync();
            var device = _x19.Device ?? await _x19.InitializeDeviceAsync();

            // 第二阶段：用户已输入短信码，且持有上一次返回的 ticket
            if (!string.IsNullOrWhiteSpace(request.SmsCode) && !string.IsNullOrWhiteSpace(request.SmsTicket))
            {
                var user = await _x19.CompleteSmsLoginAsync(request.Username, request.SmsTicket!);
                if (user == null)
                {
                    return new AuthResult
                    {
                        Channel = Channel,
                        Status = AuthStatus.Failed,
                        Account = request.Username,
                        Error = "SMS finish login returned no user payload."
                    };
                }

                var (otp, _) = await _x19.ContinueAsync(user, device);
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

            // 第一阶段：发短信
            var sent = await _x19.SendSmsCodeAsync(request.Username);
            if (!sent)
            {
                return new AuthResult
                {
                    Channel = Channel,
                    Status = AuthStatus.Failed,
                    Account = request.Username,
                    Error = "Failed to send SMS code."
                };
            }

            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.NeedsSmsVerify,
                Account = request.Username,
                MaskedMobile = request.Username,
                Error = "SMS code sent. Re-call with SmsCode to finish login."
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
