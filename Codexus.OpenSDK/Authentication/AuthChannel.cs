namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 多账号认证聚合支持的渠道枚举。
/// </summary>
public enum AuthChannel
{
    /// <summary>4399 用户名/密码登录，登录成功后产出 SAuth json 字符串。</summary>
    C4399,

    /// <summary>网易 X19 邮箱/密码登录（走 UniSdkMPay），登录成功后通过 X19.ContinueAsync 拿到 OTP。</summary>
    X19Email,

    /// <summary>网易 X19 短信登录（走 UniSdkMPay SMS），需要短信验证码二次确认。</summary>
    X19Sms,

    /// <summary>网易 X19 续登：直接喂入持久化的 SAuth json 字符串恢复会话。</summary>
    X19Resume
}
