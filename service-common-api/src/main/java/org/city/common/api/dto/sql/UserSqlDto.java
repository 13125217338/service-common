package org.city.common.api.dto.sql;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @作者 ChengShi
 * @日期 2023年5月2日
 * @版本 1.0
 * @描述 用户自定义Sql参数（防注入安全参数）
 */
@Data
@Accessors(chain = true)
public class UserSqlDto {
	/* 自定义sql追加字段 - 如：sys.create_time as createTime,xxx,等 - 只有查询有用（逗号结尾） */
	private String fields;
	/* 自定义sql追加连接表 - 如：left join system sys on c.id = sys.id - 只有查询有用 */
	private String join;
	/* 自定义sql条件 - 如：and sys.name = '插件' - 查询、删除、更新有用 */
	private String where;
	/* 自定义sql表名 - 如：system - 全部有用 */
	private String table;
	/* 自定义sql分组条件 - 如：total > 10 - 分组后有用 */
	private String having;
}
