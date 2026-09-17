using System.Collections.Concurrent;

namespace Codexus.OpenSDK.Authentication;

/// <summary>
/// 多账号内存存储。以 (渠道, EntityId) 为主键存放最近一次成功的认证结果。
/// </summary>
public sealed class AccountStore
{
    private readonly ConcurrentDictionary<(AuthChannel Channel, string EntityId), AuthResult> _items = new();

    public void Save(AuthResult result)
    {
        if (result.EntityId is null)
            return;
        _items[(result.Channel, result.EntityId)] = result;
    }

    public AuthResult? Find(AuthChannel channel, string entityId) =>
        _items.TryGetValue((channel, entityId), out var r) ? r : null;

    public IEnumerable<AuthResult> List() => _items.Values;

    public bool Remove(AuthChannel channel, string entityId) =>
        _items.TryRemove((channel, entityId), out _);

    public void Clear() => _items.Clear();
}
