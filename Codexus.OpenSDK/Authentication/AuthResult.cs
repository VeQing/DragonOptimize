namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 统一的认证结果。所有渠道的产物都被收敛到这个对象上。
/// </summary>
public sealed class AuthResult
{
    /// <summary>本次认证使用的渠道。</summary>
    public required AuthChannel Channel { get; init; }

    /// <summary>本次认证最终状态。</summary>
    public required AuthStatus Status { get; init; }

    /// <summary>账号展示名（X19 OTP 的 Account 字段或 Username）。</summary>
    public string? Account { get; init; }

    /// <summary>统一实体 ID。X19 通道来自 X19AuthenticationOtp.EntityId；C4399 通道为空（需走 X19 续登才拿到）。</summary>
    public string? EntityId { get; init; }

    /// <summary>登录 Token。X19 通道来自 X19AuthenticationOtp.Token。</summary>
    public string? Token { get; init; }

    /// <summary>SAuth json 字符串。可用于持久化、X19Resume 通道续登。</summary>
    public string? SAuthJson { get; init; }

    /// <summary>SMS 通道返回的脱敏手机号，用于让用户判断该往哪个号码发短信。</summary>
    public string? MaskedMobile { get; init; }

    /// <summary>SMS 通道验证后拿到的 ticket，二次提交时附带。</summary>
    public string? SmsTicket { get; init; }

    /// <summary>C4399 风控提示需要的会话 ID（仅占位/可携带，实际由调用方提供）。</summary>
    public string? CaptchaSessionId { get; init; }

    /// <summary>失败时的人类可读错误信息。</summary>
    public string? Error { get; init; }

    public bool IsSuccess => Status == AuthStatus.Success;
}
