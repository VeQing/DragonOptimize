namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 统一的认证请求入参。各渠道按需使用其中的字段。
/// </summary>
public sealed class AuthRequest
{
    /// <summary>登录用户名（4399 用户名、网易邮箱、手机号等，视渠道而定）。</summary>
    public required string Username { get; init; }

    /// <summary>密码（明文）。C4399 / X19Email 通道使用。</summary>
    public string? Password { get; init; }

    /// <summary>4399 图形验证码。当上一次返回 NeedsCaptcha 时附带。</summary>
    public string? Captcha { get; init; }

    /// <summary>4399 验证码会话 ID。当上一次返回 NeedsCaptcha 时附带。</summary>
    public string? CaptchaSessionId { get; init; }

    /// <summary>短信验证码。X19Sms 通道二次提交时使用。</summary>
    public string? SmsCode { get; init; }

    /// <summary>SMS 验证 ticket。X19Sms 通道二次提交时由 VerifySmsCodeAsync 返回。</summary>
    public string? SmsTicket { get; init; }

    /// <summary>持久化的 SAuth json 字符串。X19Resume 通道使用。</summary>
    public string? SAuthJson { get; init; }
}
