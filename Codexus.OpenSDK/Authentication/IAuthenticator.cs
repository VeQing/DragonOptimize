namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 单个渠道认证器的统一抽象。所有适配器（C4399、X19 Email、X19 SMS、X19 续登）都实现此接口。
/// </summary>
public interface IAuthenticator : IAsyncDisposable, IDisposable
{
    AuthChannel Channel { get; }

    Task<AuthResult> AuthenticateAsync(AuthRequest request, CancellationToken cancellationToken = default);
}
