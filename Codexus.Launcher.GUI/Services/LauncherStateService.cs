using System.Collections.ObjectModel;
using System.Text.Json;
using Codexus.OpenSDK.Authentication;

namespace Codexus.Launcher.GUI.Services;

/// <summary>
/// 服务器列表 + 已存账号 + 已存 SAuth 的本地持久化。
/// 数据放在 exe 同目录下 launcher_state.json，简单透明，方便用户排查。
/// </summary>
public sealed class LauncherStateService
{
    private static readonly string StatePath =
        Path.Combine(AppContext.BaseDirectory, "launcher_state.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public ObservableCollection<ServerEntry> Servers { get; } = new();
    public ObservableCollection<SavedAccount> Accounts { get; } = new();

    public LauncherStateService()
    {
        Load();
        if (Servers.Count == 0)
        {
            Servers.Add(new ServerEntry
            {
                Name = "默认服务器",
                Address = "45.253.142.30",
                Port = 25565,
                RoleName = "YOUR_ROLE",
                GameId = "4663909014288106690",
                GameVersion = "1.21",
                BootstrapMd5 = "684528BF492A84489F825F5599B3E1C6",
                DatMd5 = "574033E7E4841D8AC4C14D7FA5E05337",
                CrcSalt = "22AC4B0143EFFC80F2905B267D4D84D3"
            });
        }
    }

    public void Save()
    {
        try
        {
            var dto = new StateDto
            {
                Servers = Servers.ToList(),
                Accounts = Accounts.ToList()
            };
            File.WriteAllText(StatePath, JsonSerializer.Serialize(dto, JsonOpts));
        }
        catch
        {
            // 持久化失败不致命，UI 还能继续用内存态工作。
        }
    }

    private void Load()
    {
        try
        {
            if (!File.Exists(StatePath)) return;
            var dto = JsonSerializer.Deserialize<StateDto>(File.ReadAllText(StatePath), JsonOpts);
            if (dto?.Servers is { } svrs)
                foreach (var s in svrs) Servers.Add(s);
            if (dto?.Accounts is { } accs)
                foreach (var a in accs) Accounts.Add(a);
        }
        catch
        {
            // 读不出来就当全新状态。
        }
    }

    public void SaveAccount(AuthResult result)
    {
        if (string.IsNullOrEmpty(result.SAuthJson)) return;

        var idx = Accounts
            .Select((a, i) => (a, i))
            .FirstOrDefault(t => t.a.Channel == result.Channel
                && t.a.Account == (result.Account ?? result.EntityId ?? "unknown"))
            .i;

        var entry = new SavedAccount
        {
            Channel = result.Channel,
            Account = result.Account ?? result.EntityId ?? "unknown",
            EntityId = result.EntityId ?? "",
            Token = result.Token ?? "",
            SAuthJson = result.SAuthJson,
            SavedAt = DateTime.Now
        };

        if (idx >= 0) Accounts[idx] = entry;
        else Accounts.Add(entry);
        Save();
    }

    private sealed class StateDto
    {
        public List<ServerEntry>? Servers { get; set; }
        public List<SavedAccount>? Accounts { get; set; }
    }
}

public sealed class ServerEntry
{
    public string Name { get; set; } = "";
    public string Address { get; set; } = "";
    public int Port { get; set; } = 25565;
    public string RoleName { get; set; } = "YOUR_ROLE";
    public string GameId { get; set; } = "4663909014288106690";
    public string GameVersion { get; set; } = "1.21";
    public string BootstrapMd5 { get; set; } = "684528BF492A84489F825F5599B3E1C6";
    public string DatMd5 { get; set; } = "574033E7E4841D8AC4C14D7FA5E05337";
    public string CrcSalt { get; set; } = "22AC4B0143EFFC80F2905B267D4D84D3";
}

public sealed class SavedAccount
{
    public AuthChannel Channel { get; set; }
    public string Account { get; set; } = "";
    public string EntityId { get; set; } = "";
    public string Token { get; set; } = "";
    public string SAuthJson { get; set; } = "";
    public DateTime SavedAt { get; set; }
}
