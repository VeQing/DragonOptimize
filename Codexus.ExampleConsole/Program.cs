using System.CommandLine;
using Codexus.ModHost;
using Codexus.ModHost.Event;
using Codexus.OpenSDK;
using Codexus.OpenSDK.Authentication;
using Codexus.OpenSDK.Entities.Yggdrasil;
using Codexus.OpenSDK.Yggdrasil;
using Codexus.OpenTransport;
using Codexus.OpenTransport.Entities.Transport;
using Codexus.OpenTransport.Event;
using Serilog;

// 启动器 demo：通过 AuthManager 多账号认证聚合入口登录，再启动 OpenTransport 连入游戏服务器。
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .WriteTo.Console()
    .CreateLogger();

var rootCmd = BuildRootCommand();
return await rootCmd.InvokeAsync(args);

static Command BuildRootCommand()
{
    var channelOption = new Option<AuthChannel>(
        "--channel",
        description: "认证渠道：c4399 / x19-email / x19-sms / x19-resume"
    )
    { IsRequired = true };
    channelOption.AddAlias("-c");

    var usernameOption = new Option<string>("--username", "登录用户名（4399 用户名 / 网易邮箱 / 手机号）");
    usernameOption.AddAlias("-u");

    var passwordOption = new Option<string?>("--password", "密码（C4399 / X19Email 需要）");
    passwordOption.AddAlias("-p");

    var captchaOption = new Option<string?>("--captcha", "4399 图形验证码（风控触发后再次提交时附带）");
    var captchaSessionOption = new Option<string?>(
        "--captcha-session", "4399 验证码会话 ID（风控触发后再次提交时附带）");

    var smsCodeOption = new Option<string?>("--sms-code", "短信验证码（X19Sms 第二阶段提交时附带）");
    var smsTicketOption = new Option<string?>(
        "--sms-ticket", "SMS 验证 ticket（X19Sms 第一阶段返回，第二阶段提交时附带）");

    var sauthJsonOption = new Option<string?>(
        "--sauth-json",
        "X19Resume 通道续登使用的 SAuth json 字符串（或 @path 读取文件）");

    var serverOption = new Option<string>("--server", () => "45.253.142.30", "游戏服务器地址");
    var portOption = new Option<int>("--port", () => 25565, "游戏服务器端口");
    var roleOption = new Option<string>("--role", () => "YOUR_ROLE", "角色名");
    var gameIdOption = new Option<string>(
        "--game-id", () => "4663909014288106690", "游戏 ID");
    var gameVersionOption = new Option<string>(
        "--game-version", () => "1.21", "游戏版本");
    var bootstrapMd5Option = new Option<string>(
        "--bootstrap-md5", () => "684528BF492A84489F825F5599B3E1C6", "bootstrap md5");
    var datMd5Option = new Option<string>(
        "--dat-md5", () => "574033E7E4841D8AC4C14D7FA5E05337", "dat 文件 md5");
    var crcSaltOption = new Option<string>(
        "--crc-salt", () => "22AC4B0143EFFC80F2905B267D4D84D3", "Yggdrasil CrcSalt");

    var launchOption = new Option<bool>(
        "--launch",
        "认证成功后是否启动 OpenTransport 连入游戏服务器（不带则仅做认证并打印结果）");

    var modsDirOption = new Option<string>(
        "--mods-dir",
        () => Path.Combine(AppContext.BaseDirectory, "Mods"),
        "Mod 目录路径");

    var root = new RootCommand("Codexus.OpenSDK Demo Console —— 多账号认证聚合 + OpenTransport demo")
    {
        channelOption, usernameOption, passwordOption,
        captchaOption, captchaSessionOption,
        smsCodeOption, smsTicketOption,
        sauthJsonOption,
        serverOption, portOption, roleOption,
        gameIdOption, gameVersionOption,
        bootstrapMd5Option, datMd5Option, crcSaltOption,
        launchOption, modsDirOption
    };

    root.SetHandler(async ctx =>
    {
        var exit = await RunAsync(
            ctx.ParseResult.GetValueForOption(channelOption)!,
            ctx.ParseResult.GetValueForOption(usernameOption),
            ctx.ParseResult.GetValueForOption(passwordOption),
            ctx.ParseResult.GetValueForOption(captchaOption),
            ctx.ParseResult.GetValueForOption(captchaSessionOption),
            ctx.ParseResult.GetValueForOption(smsCodeOption),
            ctx.ParseResult.GetValueForOption(smsTicketOption),
            ctx.ParseResult.GetValueForOption(sauthJsonOption),
            ctx.ParseResult.GetValueForOption(serverOption)!,
            ctx.ParseResult.GetValueForOption(portOption),
            ctx.ParseResult.GetValueForOption(roleOption)!,
            ctx.ParseResult.GetValueForOption(gameIdOption)!,
            ctx.ParseResult.GetValueForOption(gameVersionOption)!,
            ctx.ParseResult.GetValueForOption(bootstrapMd5Option)!,
            ctx.ParseResult.GetValueForOption(datMd5Option)!,
            ctx.ParseResult.GetValueForOption(crcSaltOption)!,
            ctx.ParseResult.GetValueForOption(launchOption),
            ctx.ParseResult.GetValueForOption(modsDirOption)!,
            ctx.ParseResult.GetValueForOption(gameVersionOption)!,
            ctx.Console
        );
        ctx.ExitCode = exit;
    });

    return root;
}

static async Task<int> RunAsync(
    AuthChannel channel,
    string? username,
    string? password,
    string? captcha,
    string? captchaSession,
    string? smsCode,
    string? smsTicket,
    string? sauthJson,
    string server,
    int port,
    string role,
    string gameId,
    string gameVersion,
    string bootstrapMd5,
    string datMd5,
    string crcSalt,
    bool launch,
    string modsDir,
    string latestGameVersion,
    IConsole console)
{
    Log.Information("=== Codexus.OpenSDK Demo Console ===");
    Log.Information("Channel : {Channel}", channel);
    Log.Information("Username: {Username}", username ?? "(none)");

    if (!Directory.Exists(modsDir))
    {
        try { Directory.CreateDirectory(modsDir); }
        catch (Exception ex) { Log.Warning(ex, "Failed to create Mods dir at {Dir}", modsDir); }
    }
    var manager = new ModManager(Log.Logger, modsDir);
    try { manager.Initialize(); }
    catch (Exception ex) { Log.Warning(ex, "Mod manager initialize failed, continuing without mods."); }

    var auth = new AuthManager();

    // C4399 通道登录后只产出 SAuthJson；要拿到可用的 EntityId/Token，需要把 SAuthJson 再走一遍 X19Resume。
    // 这里演示了多账号聚合入口的链式能力：C4399 + X19Resume 拼出完整凭证。
    var request = BuildRequest(channel, username, password, captcha, captchaSession,
        smsCode, smsTicket, sauthJson);

    AuthResult result;
    try
    {
        result = await auth.AuthenticateAsync(channel, request);
    }
    catch (ArgumentException ex)
    {
        Log.Error("Invalid arguments: {Message}", ex.Message);
        PrintUsageHint(channel);
        return 2;
    }

    // C4399 通道成功后，需要再用 SAuthJson 走 X19Resume 才能拿到完整登录态
    if (result.IsSuccess && channel == AuthChannel.C4399 && !string.IsNullOrEmpty(result.SAuthJson))
    {
        Log.Information("C4399 login succeeded, chaining through X19Resume to obtain EntityId/Token...");
        var chainRequest = new AuthRequest
        {
            Username = username ?? "c4399-resume",
            SAuthJson = result.SAuthJson
        };
        result = await auth.AuthenticateAsync(AuthChannel.X19Resume, chainRequest);
    }

    PrintResult(result);

    if (!result.IsSuccess)
    {
        PrintRetryHint(channel, result);
        return 1;
    }

    if (!launch)
    {
        Log.Information("Skipping OpenTransport launch (--launch not set).");
        return 0;
    }

    Log.Information("Launching OpenTransport against {Server}:{Port}", server, port);
    await LaunchTransportAsync(
        result, server, port, role, gameId, gameVersion, bootstrapMd5, datMd5, crcSalt,
        latestGameVersion);
    return 0;
}

static AuthRequest BuildRequest(
    AuthChannel channel, string? username, string? password,
    string? captcha, string? captchaSession,
    string? smsCode, string? smsTicket, string? sauthJson)
{
    return channel switch
    {
        AuthChannel.C4399 => new AuthRequest
        {
            Username = username ?? throw new ArgumentException("--username is required for C4399 channel."),
            Password = password,
            Captcha = captcha,
            CaptchaSessionId = captchaSession
        },
        AuthChannel.X19Email => new AuthRequest
        {
            Username = username ?? throw new ArgumentException("--username is required for X19Email channel."),
            Password = password
        },
        AuthChannel.X19Sms => new AuthRequest
        {
            Username = username ?? throw new ArgumentException("--username (phone) is required for X19Sms channel."),
            SmsCode = smsCode,
            SmsTicket = smsTicket
        },
        AuthChannel.X19Resume => new AuthRequest
        {
            Username = username ?? "x19-resume",
            SAuthJson = ResolveSAuthJson(sauthJson)
        },
        _ => throw new ArgumentOutOfRangeException(nameof(channel))
    };
}

static string ResolveSAuthJson(string? raw)
{
    if (string.IsNullOrWhiteSpace(raw))
        throw new ArgumentException("--sauth-json is required for X19Resume channel.");
    if (raw.StartsWith('@'))
    {
        var path = raw[1..];
        if (!File.Exists(path))
            throw new FileNotFoundException($"SAuth json file not found: {path}", path);
        return File.ReadAllText(path);
    }
    return raw;
}

static void PrintResult(AuthResult result)
{
    Log.Information("--- Auth Result ---");
    Log.Information("Status  : {Status}", result.Status);
    if (!string.IsNullOrEmpty(result.Account))   Log.Information("Account : {Account}", result.Account);
    if (!string.IsNullOrEmpty(result.EntityId)) Log.Information("EntityId: {EntityId}", result.EntityId);
    if (!string.IsNullOrEmpty(result.Token))    Log.Information("Token   : {Token}", Truncate(result.Token, 24));
    if (!string.IsNullOrEmpty(result.SAuthJson)) Log.Information("SAuthJson: {Json}", Truncate(result.SAuthJson, 64));
    if (!string.IsNullOrEmpty(result.MaskedMobile)) Log.Information("MaskedMobile: {M}", result.MaskedMobile);
    if (!string.IsNullOrEmpty(result.SmsTicket)) Log.Information("SmsTicket: {T}", Truncate(result.SmsTicket, 32));
    if (!string.IsNullOrEmpty(result.Error))    Log.Information("Error   : {Error}", result.Error);
}

static string Truncate(string? s, int max) =>
    string.IsNullOrEmpty(s) ? "" : (s.Length <= max ? s : s[..max] + "...");

static void PrintUsageHint(AuthChannel channel)
{
    Log.Information("Usage hint:");
    Log.Information("  Codexus.ExampleConsole --channel {Ch} --username <user> [--password <pwd>] ...", channel);
    Log.Information("Supported channels: c4399, x19-email, x19-sms, x19-resume");
}

static void PrintRetryHint(AuthChannel channel, AuthResult result)
{
    switch (result.Status)
    {
        case AuthStatus.NeedsCaptcha:
            Log.Information("Retry hint: re-run with --captcha <code> --captcha-session <id>.");
            break;
        case AuthStatus.NeedsSmsVerify:
            Log.Information("Retry hint: re-run with --sms-code <code> --sms-ticket <ticket>.");
            break;
    }
}

static async Task LaunchTransportAsync(
    AuthResult result, string server, int port, string role,
    string gameId, string gameVersion, string bootstrapMd5, string datMd5,
    string crcSalt, string latestGameVersion)
{
    if (string.IsNullOrEmpty(result.EntityId) || string.IsNullOrEmpty(result.Token))
        throw new InvalidOperationException("EntityId/Token missing, cannot launch transport.");

    var profile = new GameProfile
    {
        GameId = gameId,
        GameVersion = gameVersion,
        BootstrapMd5 = bootstrapMd5,
        DatFileMd5 = datMd5,
        Mods = new ModList(),
        User = new UserProfile
        {
            UserId = int.Parse(result.EntityId),
            UserToken = result.Token
        }
    };

    var request = new CreateRequest
    {
        ServerAddress = server,
        ServerPort = port,
        RoleName = role,
        Debug = false
    };

    var yggdrasil = new StandardYggdrasil(new YggdrasilData
    {
        LauncherVersion = latestGameVersion,
        Channel = "netease",
        CrcSalt = crcSalt
    });

    EventBus.Instance.Subscribe<EventJoinServer>(async e =>
    {
        await Task.Run(async () =>
        {
            var join = await yggdrasil.JoinServerAsync(e.Context.Session.Profile, e.ServerId);
            if (join.IsSuccess) Log.Information("Joined server successfully.");
            else Log.Error("Joined server failed: {Error}", join.Error);
        }).ConfigureAwait(false);
    });

    var transport = OpenTransport.Create(profile, request, Log.Logger);
    await transport.StartAsync();
    Console.WriteLine("Press any key to exit...");
    Console.ReadKey();
}
