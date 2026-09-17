namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 一次认证尝试的最终状态。
/// </summary>
public enum AuthStatus
{
    /// <summary>认证成功，已拿到可用的 EntityId/Token。</summary>
    Success,

    /// <summary>风控要求输入图形验证码（C4399 通道）。调用方需要在下一次请求带上 Captcha 与 CaptchaSessionId。</summary>
    NeedsCaptcha,

    /// <summary>需要短信验证码二次确认（X19 SMS 通道）。调用方需要先发短信、再带上 SmsCode 重新调用。</summary>
    NeedsSmsVerify,

    /// <summary>认证失败。错误信息记录在 AuthResult.Error。</summary>
    Failed
}
