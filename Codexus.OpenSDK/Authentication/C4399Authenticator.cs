using Codexus.OpenSDK.Exceptions;

namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 4399 通道适配器。用户名/密码登录，产出 SAuth json 字符串。
/// </summary>
internal sealed class C4399Authenticator : IAuthenticator
{
    private readonly C4399 _c4399 = new();

    public AuthChannel Channel => AuthChannel.C4399;

    public async Task<AuthResult> AuthenticateAsync(AuthRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);
        if (string.IsNullOrWhiteSpace(request.Password))
            throw new ArgumentException("Password is required for C4399 channel.", nameof(request));

        cancellationToken.ThrowIfCancellationRequested();
        try
        {
            var cookie = await _c4399.LoginWithPasswordAsync(
                request.Username,
                request.Password!,
                request.CaptchaSessionId,
                request.Captcha);

            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.Success,
                Account = request.Username,
                SAuthJson = cookie
            };
        }
        catch (VerifyException)
        {
            return new AuthResult
            {
                Channel = Channel,
                Status = AuthStatus.NeedsCaptcha,
                Account = request.Username,
                Error = "Captcha required by 4399 risk control."
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
        _c4399.Dispose();
        GC.SuppressFinalize(this);
    }

    public ValueTask DisposeAsync()
    {
        _c4399.Dispose();
        return ValueTask.CompletedTask;
    }
}
