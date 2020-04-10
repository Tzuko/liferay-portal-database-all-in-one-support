/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package it.dontesta.labs.liferay.portal.dao.db;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.dao.db.BaseDB;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.io.unsync.UnsyncBufferedReader;
import com.liferay.portal.kernel.io.unsync.UnsyncStringReader;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

/**
 * @author Alexander Chow
 * @author Sandeep Soni
 * @author Ganesh Ram
 * @author Antonio Musarra
 * @author Javier Alpanez
 */
public class SQLServerDB extends BaseDB {

	public SQLServerDB(int majorVersion, int minorVersion) {
		super(DBType.SQLSERVER, majorVersion, minorVersion);
	}
	
	@Override
	public String getPopulateSQL(String databaseName, String sqlContent) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("use ");
		sb.append(databaseName);
		sb.append(";\n\n");
		sb.append(sqlContent);
		
		return sb.toString();
	}

	@Override
	public String getRecreateSQL(String databaseName) {
		StringBundler sb = new StringBundler(17);

		sb.append("drop database ");
		sb.append(databaseName);
		sb.append(";\n");
		sb.append("create database ");
		sb.append(databaseName);
		sb.append(";\n");
		sb.append("\n");
		sb.append("go\n");
		sb.append("\n");
		
		return sb.toString();
	}

	@Override
	public String buildSQL(String template) throws IOException, SQLException {
		template = replaceTemplate(template);

		template = reword(template);
		template = StringUtil.replace(template, "\ngo;\n", "\ngo\n");
		template = StringUtil.replace(template, new String[] { "\\\\", "\\'", "\\\"", "\\n", "\\r" },
				new String[] { "\\", "''", "\"", "\n", "\r" });

		return template;
	}

	@Override
	protected int[] getSQLTypes() {
		return _SQL_TYPES;
	}

	@Override
	protected String[] getTemplate() {
		return _SQL_SERVER;
	}

	@Override
	protected String reword(String data) throws IOException, SQLException {
		try (UnsyncBufferedReader unsyncBufferedReader = new UnsyncBufferedReader(new UnsyncStringReader(data))) {

			StringBundler sb = new StringBundler();

			String line = null;

			while ((line = unsyncBufferedReader.readLine()) != null) {
				if (line.startsWith(ALTER_COLUMN_NAME)) {
					String[] template = buildColumnNameTokens(line);

					line = StringUtil.replace("exec sp_rename '@table@.@old-column@', " + "'@new-column@', 'column';",
							REWORD_TEMPLATE, template);
				} else if (line.startsWith(ALTER_COLUMN_TYPE)) {
					String[] template = buildColumnTypeTokens(line);

					line = StringUtil.replace("alter table @table@ alter column @old-column@ @type@;", REWORD_TEMPLATE,
							template);
				} else if (line.startsWith(ALTER_TABLE_NAME)) {
					String[] template = buildTableNameTokens(line);

					line = StringUtil.replace("exec sp_rename '@old-table@', '@new-table@';", RENAME_TABLE_TEMPLATE,
							template);
				} else if (line.contains(DROP_INDEX)) {
					String[] tokens = StringUtil.split(line, ' ');

					String tableName = tokens[4];

					if (tableName.endsWith(StringPool.SEMICOLON)) {
						tableName = tableName.substring(0, tableName.length() - 1);
					}

					line = StringUtil.replace("drop index @table@.@index@;", "@table@", tableName);
					line = StringUtil.replace(line, "@index@", tokens[2]);
				}

				sb.append(line);
				sb.append("\n");
			}

			return sb.toString();
		}
	}
	

	private static final String[] _SQL_SERVER = { "--", "1", "0", "'19700101'", "GetDate()", " image", " image", " bit",
			" datetime", " float", " int", " bigint", " nvarchar(4000)", " nvarchar(max)", " nvarchar",
			"  identity(1,1)", "go" };

	private static final int[] _SQL_TYPES = { Types.LONGVARBINARY, Types.LONGVARBINARY, Types.BIT, Types.TIMESTAMP,
			Types.DOUBLE, Types.INTEGER, Types.BIGINT, Types.NVARCHAR, Types.NVARCHAR, Types.NVARCHAR };

}
