using Codexus.OpenSDK.Entities.MPay;

namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 网易 X19 续登通道适配器。直接喂入持久化的 SAuth json 字符串恢复会话，
/// 跳过 LoginWithEmail/SMS 环节，直接走 X19.ContinueAsync(json)。
/// </summary>
internal sealed class X19ResumeAuthenticator : IAuthenticator
{
    private readonly X19 _x19 = new();

    public AuthChannel Channel => AuthChannel.X19Resume;

    public async Task<AuthResult> AuthenticateAsync(AuthRequest request, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(request);
        if (string.IsNullOrWhiteSpace(request.SAuthJson))
            throw new ArgumentException("SAuthJson is required for X19Resume channel.", nameof(request));

        cancellationToken.ThrowIfCancellationRequested();
        try
        {
            var (otp, _) = await _x19.ContinueAsync(request.SAuthJson!);
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
