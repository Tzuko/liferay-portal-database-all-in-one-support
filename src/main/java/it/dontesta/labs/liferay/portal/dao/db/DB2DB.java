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
 * @author Bruno Farache
 * @author Antonio Musarra
 * @author Javier Alpanez
 */
public class DB2DB extends BaseDB {

	public DB2DB(int majorVersion, int minorVersion) {
		super(DBType.DB2, majorVersion, minorVersion);
	}

	@Override
	public String getPopulateSQL(String databaseName, String sqlContent) {
		StringBundler sb = new StringBundler(4);

		sb.append("connect to ");
		sb.append(databaseName);
		sb.append(";\n");
		sb.append(sqlContent);

		return sb.toString();
	}

	@Override
	public String getRecreateSQL(String databaseName) {
		
		StringBundler sb = new StringBundler();
		sb.append("drop database ");
		sb.append(databaseName);
		sb.append(";\n");
		sb.append("create database ");
		sb.append(databaseName);
		sb.append(" pagesize 32768 temporary tablespace managed by automatic ");
		sb.append("storage;\n");
		
		return sb.toString();
	}

	@Override
	public String buildSQL(String template) throws IOException, SQLException {
		template = replaceTemplate(template);
		template = reword(template);
		
		
		template = StringUtil.replace(template, "\\'", "''");
		template = StringUtil.replace(template, "\\n", "'||CHR(10)||'");

		return template;
	}

	@Override
	protected int[] getSQLTypes() {
		return _SQL_TYPES;
	}

	@Override
	protected String[] getTemplate() {
		return _DB2;
	}

	@Override
	protected String reword(String data) throws IOException, SQLException {
		try (UnsyncBufferedReader unsyncBufferedReader =
				new UnsyncBufferedReader(new UnsyncStringReader(data))) {

			StringBundler sb = new StringBundler();

			String line = null;

			while ((line = unsyncBufferedReader.readLine()) != null) {
				if (line.startsWith(ALTER_COLUMN_NAME)) {
					String[] template = buildColumnNameTokens(line);

					line = StringUtil.replace(
						"alter table @table@ add column @new-column@ @type@;\n",
						REWORD_TEMPLATE, template);
					line += StringUtil.replace(
						"update @table@ set @new-column@ = @old-column@;\n",
						REWORD_TEMPLATE, template);
					line += StringUtil.replace(
						"alter table @table@ drop column @old-column@",
						REWORD_TEMPLATE, template);
				}
				else if (line.startsWith(ALTER_TABLE_NAME)) {
					String[] template = buildTableNameTokens(line);

					line = StringUtil.replace(
						"alter table @old-table@ to @new-table@;",
						RENAME_TABLE_TEMPLATE, template);
				}
				else if (line.contains(DROP_INDEX)) {
					String[] tokens = StringUtil.split(line, ' ');

					line = StringUtil.replace(
						"drop index @index@;", "@index@", tokens[2]);
				}

				sb.append(line);
				sb.append("\n");
			}

			return sb.toString();
		}
	}
	

	private static final String[] _DB2 = {
		"--", "1", "0", "'1970-01-01-00.00.00.000000'", "current timestamp",
		" blob", " blob", " smallint", " timestamp", " double", " integer",
		" bigint", " varchar(4000)", " clob", " varchar",
		" generated always as identity", "commit"
	};
	
	private static final int[] _SQL_TYPES = {
			Types.BLOB, Types.BLOB, Types.SMALLINT, Types.TIMESTAMP, Types.DOUBLE,
			Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.CLOB, Types.VARCHAR
	};
}
