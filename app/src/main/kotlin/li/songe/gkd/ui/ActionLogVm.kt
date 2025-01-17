package li.songe.gkd.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.data.Tuple3
import li.songe.gkd.db.DbSet
import li.songe.gkd.util.subsIdToRawFlow

class ActionLogVm : ViewModel() {

    val pagingDataFlow = Pager(PagingConfig(pageSize = 100)) { DbSet.actionLogDao.pagingSource() }
        .flow
        .combine(subsIdToRawFlow) { pagingData, subsIdToRaw ->
            pagingData.map { c ->
                val group = if (c.groupType == SubsConfig.AppGroupType) {
                    val app = subsIdToRaw[c.subsId]?.apps?.find { a -> a.id == c.appId }
                    app?.groups?.find { g -> g.key == c.groupKey }
                } else {
                    subsIdToRaw[c.subsId]?.globalGroups?.find { g -> g.key == c.groupKey }
                }
                val rule = group?.rules?.run {
                    if (c.ruleKey != null) {
                        find { r -> r.key == c.ruleKey }
                    } else {
                        getOrNull(c.ruleIndex)
                    }
                }
                Tuple3(c, group, rule)
            }
        }
        .cachedIn(viewModelScope)

    val actionLogCountFlow =
        DbSet.actionLogDao.count().stateIn(viewModelScope, SharingStarted.Eagerly, 0)

}