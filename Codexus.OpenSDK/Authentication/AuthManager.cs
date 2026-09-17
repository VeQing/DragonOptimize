namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 多账号认证聚合入口。负责按 <see cref="AuthChannel"/> 路由到对应的 <see cref="IAuthenticator"/>，
/// 并维护一个内存中的多账号存储 <see cref="AccountStore"/>。
/// 这是「多账号认证聚合」的统一入口：调用方不再直接 new C4399 / new X19，
/// 而是通过本类按渠道拿到结果，并持久化到 <see cref="Store"/>。
/// </summary>
public sealed class AuthManager : IAsyncDisposable
{
    /// <summary>本管理器支持的渠道列表。</summary>
    public static IReadOnlyCollection<AuthChannel> SupportedChannels { get; } =
        [AuthChannel.C4399, AuthChannel.X19Email, AuthChannel.X19Sms, AuthChannel.X19Resume];

    /// <summary>多账号内存存储。</summary>
    public AccountStore Store { get; } = new();

    /// <summary>构造一个对应渠道的认证器（每次新建实例，调用方负责释放）。</summary>
    public static IAuthenticator CreateAuthenticator(AuthChannel channel) => channel switch
    {
        AuthChannel.C4399 => new C4399Authenticator(),
        AuthChannel.X19Email => new X19EmailAuthenticator(),
        AuthChannel.X19Sms => new X19SmsAuthenticator(),
        AuthChannel.X19Resume => new X19ResumeAuthenticator(),
        _ => throw new ArgumentOutOfRangeException(nameof(channel), channel, "Unsupported auth channel.")
    };

    /// <summary>
    /// 执行一次认证，成功时自动存入 <see cref="Store"/>。
    /// 使用 <see cref="CreateAuthenticator"/> 创建实例，调用结束后释放。
    /// </summary>
    public async Task<AuthResult> AuthenticateAsync(
        AuthChannel channel, AuthRequest request, CancellationToken cancellationToken = default)
    {
        await using var auth = CreateAuthenticator(channel);
        var result = await auth.AuthenticateAsync(request, cancellationToken);
        if (result.IsSuccess)
            Store.Save(result);
        return result;
    }

    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}
