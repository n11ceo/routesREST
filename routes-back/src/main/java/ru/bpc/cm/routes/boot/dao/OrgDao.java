package ru.bpc.cm.routes.boot.dao;

import ru.bpc.cm.items.routes.OrgItem;

import java.util.List;

public interface OrgDao {
    List<OrgItem> getOrgs();
}
