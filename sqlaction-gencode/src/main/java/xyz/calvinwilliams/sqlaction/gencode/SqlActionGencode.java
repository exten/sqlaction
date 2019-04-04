/*
 * sqlaction - SQL action object auto-gencode tool based JDBC for Java
 * author	: calvin
 * email	: calvinwilliams@163.com
 *
 * See the file LICENSE in base directory.
 */

package xyz.calvinwilliams.sqlaction.gencode;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.LinkedList;

public class SqlActionGencode {

	final private static String				SQLACTION_VERSION = "0.0.8.0" ;
	
	final private static String				SELECT_COUNT___ = "count(*)" ;
	final private static String				COUNT___ = "count___" ;
	
	public static void main(String[] args) {
		Path					currentPath ;
		Path					sqlactionConfJsonFilePath ;
		String					sqlactionConfJsonFileContent ;
		SqlActionConf			sqlactionConf ;
		Path					dbserverConfJsonFilePath ;
		String					dbserverConfJsonFileContent ;
		DbServerConf			dbserverConf ;
		
		Connection				conn = null ;
		SqlActionDatabase		database = null ;
		
		int						nret = 0 ;
		
		try {
			System.out.println( "//////////////////////////////////////////////////////////////////////////////" );
			System.out.println( "/// sqlaction v"+SQLACTION_VERSION );
			System.out.println( "/// Copyright by calvin<calvinwilliams@163.com,calvinwilliams@gmail.com>" );
			System.out.println( "//////////////////////////////////////////////////////////////////////////////" );
			
			// Load sqlaction.conf.json
			currentPath = Paths.get(System.getProperty("user.dir")) ;
			
			while( true ) {
				try {
					sqlactionConfJsonFilePath = Paths.get(currentPath.toString(),"sqlaction.conf.json") ;
					sqlactionConfJsonFileContent = new String(Files.readAllBytes(sqlactionConfJsonFilePath)) ;
					break;
				} catch (IOException e) {
					currentPath = currentPath.getParent() ;
					if( currentPath == null ) {
						System.out.println( "*** ERROR : sqlaction.conf.json not found" );
						return;
					}
				}
			}
			
			sqlactionConf = OKJSON.stringToObject( sqlactionConfJsonFileContent, SqlActionConf.class, OKJSON.OKJSON_OTIONS_DIRECT_ACCESS_PROPERTY_ENABLE ) ;
			if( sqlactionConf == null ) {
				System.out.println(sqlactionConfJsonFilePath+" content invalid , errcode["+OKJSON.getErrorCode()+"] errdesc["+OKJSON.getErrorCode()+"]");
				return;
			}
			
			// Load dbserver.conf.json
			while( true ) {
				try {
					dbserverConfJsonFilePath = Paths.get(currentPath.toString(),"dbserver.conf.json") ;
					dbserverConfJsonFileContent = new String(Files.readAllBytes(dbserverConfJsonFilePath)) ;
					break;
				} catch (IOException e) {
					currentPath = currentPath.getParent() ;
					if( currentPath == null ) {
						System.out.println( "*** ERROR : sqlaction.conf.json not found" );
						return;
					}
				}
			}
			
			dbserverConf = OKJSON.stringToObject( dbserverConfJsonFileContent, DbServerConf.class, OKJSON.OKJSON_OTIONS_DIRECT_ACCESS_PROPERTY_ENABLE ) ;
			if( dbserverConf == null ) {
				System.out.println(dbserverConfJsonFilePath+" content invalid");
				return;
			}
			
			if( dbserverConf.dbms == null ) {
				String[] sa = dbserverConf.url.split( ":" ) ;
				if( sa.length < 3 ) {
					System.out.println( "dbserverConf.url["+dbserverConf.dbms+"] invalid" );
					return;
				}
				
				dbserverConf.dbms = sa[1] ;
			}
			
			if( ! dbserverConf.dbms.equals("mysql") ) {
				System.out.println( "dbserverConf.dbms["+dbserverConf.dbms+"] not support" );
				return;
			}
			
			System.out.println( "--- dbserverConf ---" );
			System.out.println( "  dbms["+dbserverConf.dbms+"]" );
			System.out.println( "driver["+dbserverConf.driver+"]" );
			System.out.println( "   url["+dbserverConf.url+"]" );
			System.out.println( "  user["+dbserverConf.user+"]" );
			System.out.println( "   pwd["+dbserverConf.pwd+"]" );
			
			System.out.println( "--- sqlactionConf ---" );
			System.out.println( " database["+sqlactionConf.database+"]" );
			for( SqlActionTableConf tc : sqlactionConf.tables ) {
				System.out.println( "\t" + "table["+tc.table+"]" );
				for( String s : tc.sqlactions ) {
					System.out.println( "\t\t" + "sqlaction["+s+"]" );
				}
			}
			
			// Query database metadata
			Class.forName( dbserverConf.driver );
			conn = DriverManager.getConnection( dbserverConf.url, dbserverConf.user, dbserverConf.pwd ) ;
			
			database = new SqlActionDatabase() ;
			database.databaseName = sqlactionConf.database ;
			database.tableList = new LinkedList<SqlActionTable>() ;
			
			// Generate class code
			for( SqlActionTableConf tc : sqlactionConf.tables ) {
				// Get the table in the database
				System.out.println( "SqlActionTable.getTableInDatabase["+tc.table+"] ..." );
				nret = SqlActionTable.getTableInDatabase( dbserverConf, sqlactionConf, conn, database, tc.table ) ;
				if( nret != 0 ) {
					System.out.println( "*** ERROR : SqlActionTable.getTableInDatabase["+tc.table+"] failed["+nret+"]" );
					conn.close();
					return;
				} else {
					System.out.println( "SqlActionTable.getTableInDatabase["+tc.table+"] ok" );
				}
				
				// Show all databases and tables and columns and indexes
				nret = SqlActionTable.travelTable( dbserverConf, sqlactionConf, database, tc.table, 1 ) ;
				if( nret != 0 ) {
					System.out.println( "*** ERROR : SqlActionTable.travelTable["+tc.table+"] failed["+nret+"]" );
					return;
				} else {
					System.out.println( "SqlActionTable.travelTable["+tc.table+"] ok" );
				}
				
				// query table
				SqlActionTable table = SqlActionTable.findTable( database.tableList, tc.table ) ;
				if( table == null ) {
					System.out.println( "\t" + "*** ERROR : table["+tc.table+"] not found in database["+sqlactionConf.database+"]" );
					return;
				}
				
				System.out.println( "*** NOTICE : Prepare "+Paths.get(table.javaFileName)+" output buffer ..." );
				
				StringBuilder out = new StringBuilder() ;
				
				out.append( "// This file generated by sqlaction v"+SQLACTION_VERSION+"\n" );
				out.append( "\n" );
				out.append( "package "+sqlactionConf.javaPackage+";\n" );
				out.append( "\n" );
				out.append( "import java.math.*;\n" );
				out.append( "import java.util.*;\n" );
				out.append( "import java.sql.Time;\n" );
				out.append( "import java.sql.Timestamp;\n" );
				out.append( "import java.sql.Connection;\n" );
				out.append( "import java.sql.Statement;\n" );
				out.append( "import java.sql.PreparedStatement;\n" );
				out.append( "import java.sql.ResultSet;\n" );
				out.append( "\n" );
				out.append( "public class "+table.javaClassName+" {\n" );
				
				out.append( "\n" );
				for( SqlActionColumn c : table.columnList ) {
					SqlActionColumn.dumpDefineProperty( c, out );
				}
				out.append( "\n" );
				out.append("\t").append("int				").append(COUNT___).append(" ; // defining for 'SELECT COUNT(*)'\n");
				
				// Parse sql actions and dump gencode
				for( String sqlaction : tc.sqlactions ) {
					String		sql ;
					String		methodName ;
					int			beginMetaData ;
					int			endMetaData ;
					
					sqlaction = sqlaction.trim() ;
					
					beginMetaData = sqlaction.indexOf( "@@" ) ;
					if( beginMetaData >= 0 ) {
						sql = sqlaction.substring( 0, beginMetaData ) ;
					} else {
						sql = sqlaction ;
					}
					
					beginMetaData = sqlaction.indexOf( "@@METHOD(" ) ;
					if( beginMetaData >= 0 ) {
						endMetaData = sqlaction.indexOf( ")", beginMetaData ) ;
						if( endMetaData == -1 ) {
							System.out.println( "sql["+sql+"] invalid" );
							return;
						}
						methodName = sqlaction.substring( beginMetaData+9, endMetaData ) ;
					} else {
						methodName = SqlActionUtil.sqlConvertToMethodName(sql) ;
					}
					
					// Parse sql action
					System.out.println( "Parse sql action ["+sqlaction+"]" );
					System.out.println( "\t" + "sql["+sql+"]" );
					System.out.println( "\t" + "methodName["+methodName+"]" );
					
					SqlActionSyntaxParser parser = new SqlActionSyntaxParser() ;
					nret = parser.parseSyntax(sql) ;
					if( nret != 0 ) {
						System.out.println( "\t" + "*** ERROR : SqlActionSyntaxParser.ParseSyntax failed["+nret+"]" );
						return;
					}
					
					if( parser.selectAllColumn == true ) {
						for( SqlActionFromTableToken tt : parser.fromTableTokenList ) {
							for( SqlActionColumn c : table.columnList ) {
								SqlActionSelectColumnToken ct = new SqlActionSelectColumnToken() ;
								ct.tableName = tt.tableName ;
								ct.tableAliasName = tt.tableAliasName ;
								ct.column = c ;
								ct.columnName = c.columnName ;
								parser.selectColumnTokenList.add( ct );
							}
						}
					}
					
					// Postpro parser I
					System.out.println( "Postpro parser I ["+sql+"]" );
					
					for( SqlActionSelectColumnToken ct : parser.selectColumnTokenList ) {
						if( ct.tableAliasName != null ) {
							if( parser.isFromTableNameExist(ct.tableAliasName) ) {
								ct.tableName = ct.tableAliasName ;
							} else {
								ct.tableName = parser.findFromTableFromAliasName(ct.tableAliasName) ;
								if( ct.tableName == null ) {
									System.out.println( "\t" + "tableAliasName["+ct.tableAliasName+"] not found in sql["+sql+"] at SELECT" );
									return;
								}
							}
						}
					}
					
					for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
						if( ct.tableAliasName != null ) {
							if( parser.isFromTableNameExist(ct.tableAliasName) ) {
								ct.tableName = ct.tableAliasName ;
							} else {
								ct.tableName = parser.findFromTableFromAliasName(ct.tableAliasName) ;
								if( ct.tableName == null ) {
									System.out.println( "\t" + "tableAliasName["+ct.tableAliasName+"] not found in sql["+sql+"] at WHERE" );
									return;
								}
							}
						}
						
						if( ct.tableAliasName2 != null ) {
							if( parser.isFromTableNameExist(ct.tableAliasName2) ) {
								ct.tableName2 = ct.tableAliasName2 ;
							} else {
								ct.tableName2 = parser.findFromTableFromAliasName(ct.tableAliasName2) ;
								if( ct.tableName2 == null ) {
									System.out.println( "\t" + "tableAliasName2["+ct.tableAliasName2+"] not found in sql["+sql+"] at WHERE" );
									return;
								}
							}
						}
					}
					
					// Show parser result
					System.out.println( "Show parser result ["+sql+"]" );
					
					System.out.println( "\t" + "selectHint["+parser.selectHint+"]" );
					
					System.out.println( "\t" + "selectAllColumn["+parser.selectAllColumn+"]" );
					
					for( SqlActionSelectColumnToken ct : parser.selectColumnTokenList ) {
						System.out.println( "\t" + "selectColumnToken.tableName["+ct.tableName+"] .tableAliasName["+ct.tableAliasName+"] .columnName["+ct.columnName+"]" );
					}
					
					for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
						System.out.println( "\t" + "fromTableToken.tableName["+ct.tableName+"] .tableAliasName["+ct.tableAliasName+"]" );
					}
					
					System.out.println( "\t" + "insertTableName["+parser.insertTableName+"]" );
					
					System.out.println( "\t" + "updateTableName["+parser.updateTableName+"]" );
					
					for( SqlActionSetColumnToken ct : parser.setColumnTokenList ) {
						System.out.println( "\t" + "setColumnToken.tableName["+ct.tableName+"] .column["+ct.columnName+"] .columnValue["+ct.columnValue+"]" );
					}
					
					System.out.println( "\t" + "deleteTableName["+parser.deleteTableName+"]" );
					
					for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
						System.out.println( "\t" + "whereColumnToken.tableName["+ct.tableName+"] .columnName["+ct.columnName+"] .operator["+ct.operator+"] .tableName2["+ct.tableName2+"] .columnName2["+ct.columnName2+"]" );
					}
					
					System.out.println( "\t" + "parser.otherTokens["+parser.otherTokens+"]" );
					
					// Postpro parser II
					System.out.println( "Postpro parser II ["+sql+"]" );
					
					for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
						// Get the table in the database
						System.out.println( "SqlActionTable.getTableInDatabase["+ct.tableName+"] ..." );
						nret = SqlActionTable.getTableInDatabase( dbserverConf, sqlactionConf, conn, database, ct.tableName ) ;
						if( nret != 0 ) {
							System.out.println( "*** ERROR : SqlActionTable.getTableInDatabase["+ct.tableName+"] failed["+nret+"]" );
							conn.close();
							return;
						} else {
							System.out.println( "SqlActionTable.getTableInDatabase["+ct.tableName+"] ok" );
						}
						
						// Show all databases and tables and columns and indexes
						nret = SqlActionTable.travelTable( dbserverConf, sqlactionConf, database, ct.tableName, 1 ) ;
						if( nret != 0 ) {
							System.out.println( "*** ERROR : SqlActionTable.travelTable["+ct.tableName+"] failed["+nret+"]" );
							return;
						} else {
							System.out.println( "SqlActionTable.travelTable["+ct.tableName+"] ok" );
						}
					}
					
					// Postpro parser III
					System.out.println( "Postpro parser III ["+sql+"]" );
					
					for( SqlActionSelectColumnToken ct : parser.selectColumnTokenList ) {
						if( ct.table == null ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.table = SqlActionTable.findTable( database.tableList, ct.tableName ) ;
								if( ct.table == null ) {
									System.out.println( "\t" + "other table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at SELECT" );
									return;
								}
							} else {
								ct.table = table ;
								if( ct.table == null ) {
									System.out.println( "\t" + "table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at SELECT" );
									return;
								}
							}
						}
						
						if( ct.column == null && ! ct.columnName.equalsIgnoreCase(SELECT_COUNT___) ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.column = SqlActionColumn.findColumn( ct.table.columnList, ct.columnName ) ;
								if( ct.column == null ) {
									System.out.println( "\t" + "other column["+ct.columnName+"] not found in table["+ct.tableName+"] in sql["+sql+"] at SELECT" );
									return;
								}
							} else {
								ct.column = SqlActionColumn.findColumn( table.columnList, ct.columnName ) ;
								if( ct.column == null ) {
									System.out.println( "\t" + "column["+ct.columnName+"] not found in table["+table.tableName+"] in sql["+sql+"] at SELECT" );
									return;
								}
							}
						}
					}
					
					for( SqlActionSetColumnToken ct : parser.setColumnTokenList ) {
						if( ct.table == null ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.table = SqlActionTable.findTable( database.tableList, ct.tableName ) ;
								if( ct.table == null ) {
									System.out.println( "\t" + "other table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at SET" );
									return;
								}
							} else {
								ct.table = table ;
								if( ct.table == null ) {
									System.out.println( "\t" + "table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at SET" );
									return;
								}
							}
						}
						
						if( ct.column == null && ! ct.columnName.equals("?") ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.column = SqlActionColumn.findColumn( ct.table.columnList, ct.columnName ) ;
								if( ct.column == null ) {
									System.out.println( "\t" + "other column["+ct.columnName+"] not found in table["+ct.tableName+"] in sql["+sql+"] at SET" );
									return;
								}
							} else {
								ct.column = SqlActionColumn.findColumn( table.columnList, ct.columnName ) ;
								if( ct.column == null ) {
									System.out.println( "\t" + "column["+ct.columnName+"] not found in table["+table.tableName+"] in sql["+sql+"] at SET" );
									return;
								}
							}
						}
					}
					
					for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
						if( ct.table == null ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.table = SqlActionTable.findTable( database.tableList, ct.tableName ) ;
								if( ct.table == null ) {
									System.out.println( "\t" + "other table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at FROM" );
									return;
								}
							} else {
								ct.table = table ;
								if( ct.table == null ) {
									System.out.println( "\t" + "table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at FROM" );
									return;
								}
							}
						}
					}
					
					for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
						if( ct.table == null ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.table = SqlActionTable.findTable( database.tableList, ct.tableName ) ;
								if( ct.table == null ) {
									System.out.println( "\t" + "other table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at WHERE" );
									return;
								}
							} else {
								ct.table = table ;
								if( ct.table == null ) {
									System.out.println( "\t" + "table["+ct.tableName+"] not found in database["+sqlactionConf.database+"] at WHERE" );
									return;
								}
							}
						}
						
						if( ct.column == null ) {
							if( ct.tableName != null && ! ct.tableName.equalsIgnoreCase(table.tableName) ) {
								ct.column = SqlActionColumn.findColumn( ct.table.columnList, ct.columnName ) ;
								if( ct.column == null ) {
									System.out.println( "\t" + "othercolumn["+ct.columnName+"] not found in table["+ct.tableName+"] in sql["+sql+"] at WHERE" );
									return;
								}
							} else {
								ct.column = SqlActionColumn.findColumn( table.columnList, ct.columnName ) ;
							}
						}
						
						if( ct.table2 == null ) {
							if( ct.tableName2 != null && ! ct.tableName2.equalsIgnoreCase(table.tableName) ) {
								ct.table2 = SqlActionTable.findTable( database.tableList, ct.tableName2 ) ;
								if( ct.table2 == null ) {
									System.out.println( "\t" + "other table2["+ct.tableName2+"] not found in database["+sqlactionConf.database+"] at WHERE" );
									return;
								}
							} else {
								ct.table2 = table ;
								if( ct.table2 == null ) {
									System.out.println( "\t" + "table2["+ct.tableName2+"] not found in database["+sqlactionConf.database+"] at WHERE" );
									return;
								}
							}
						}
						
						if( ct.column2 == null && ! ct.columnName2.equals("?") ) {
							if( ct.tableName2 != null && ! ct.tableName2.equalsIgnoreCase(table.tableName) ) {
								ct.column2 = SqlActionColumn.findColumn( ct.table2.columnList, ct.columnName2 ) ;
								if( ct.column2 == null ) {
									System.out.println( "\t" + "other column2["+ct.columnName2+"] not found in table["+ct.tableName2+"] in sql["+sql+"] at WHERE" );
									return;
								}
							} else {
								ct.column2 = SqlActionColumn.findColumn( table.columnList, ct.columnName2 ) ;
							}
						}
					}
					
					// Dump gencode
					System.out.println( "Dump gencode ["+sql+"]" );
					
					if( parser.selectColumnTokenList != null && parser.selectColumnTokenList.size() > 0 ) {
						nret = selectSqlDumpGencode( dbserverConf, sqlactionConf, tc, sql, methodName, parser, database, table, out ) ;
						if( nret != 0 ) {
							System.out.println( "\t" + "*** ERROR : SelectSqlDumpGencode failed["+nret+"]" );
							return;
						} else {
							System.out.println( "\t" + "SelectSqlDumpGencode ok" );
						}
					} else if( parser.insertTableName != null ) {
						nret = insertSqlDumpGencode( dbserverConf, sqlactionConf, tc, sql, methodName, parser, database, table, out ) ;
						if( nret != 0 ) {
							System.out.println( "\t" + "*** ERROR : InsertSqlDumpGencode failed["+nret+"]" );
							return;
						} else {
							System.out.println( "\t" + "InsertSqlDumpGencode ok" );
						}
					} else if( parser.updateTableName != null ) {
						nret = updateSqlDumpGencode( dbserverConf, sqlactionConf, tc, sql, methodName, parser, database, table, out ) ;
						if( nret != 0 ) {
							System.out.println( "\t" + "*** ERROR : UpdateSqlDumpGencode failed["+nret+"]" );
							return;
						} else {
							System.out.println( "\t" + "UpdateSqlDumpGencode ok" );
						}
					} else if( parser.deleteTableName != null ) {
						nret = deleteSqlDumpGencode( dbserverConf, sqlactionConf, tc, sql, methodName, parser, database, table, out ) ;
						if( nret != 0 ) {
							System.out.println( "\t" + "*** ERROR : DeleteSqlDumpGencode failed["+nret+"]" );
							return;
						} else {
							System.out.println( "\t" + "DeleteSqlDumpGencode ok" );
						}
					} else {
						System.out.println( "\t" + "Action["+sqlaction+"] invalid" );
						return;
					}
				}
				
				out.append( "}\n" );
				
				Files.write( Paths.get(table.javaFileName) , out.toString().getBytes() );
				
				System.out.println( "*** NOTICE : Write "+Paths.get(table.javaFileName)+" completed!!!" );
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static int selectSqlDumpGencode( DbServerConf dbserverConf, SqlActionConf sqlactionConf, SqlActionTableConf sqlactionTableConf, String sql, String methodName, SqlActionSyntaxParser parser, SqlActionDatabase database, SqlActionTable table, StringBuilder out ) {
		
		StringBuilder		methodParameters = new StringBuilder() ;
		int					nret = 0 ;
		
		methodParameters.append( "Connection conn" );
		for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
			methodParameters.append( ", List<"+ct.table.javaClassName+"> "+ct.table.javaObjectName+"ListForSelectOutput" );
		}
		for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
			methodParameters.append( ", "+ct.table.javaClassName+" "+ct.table.javaObjectName+"ForWhereInput" );
		}
		
		out.append( "\n" );
		out.append( "\t" + "// "+sql+"\n" );
		if( parser.whereColumnTokenList.size() > 0 ) {
			out.append( "\t" + "public static int " + methodName + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
			int	columnIndex = 0 ;
			for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
				columnIndex++;
				if( ct.columnName2.equals("?") ) {
					nret = SqlActionColumn.dumpWhereInputColumn( columnIndex, ct.column, ct.table.javaObjectName+"ForWhereInput"+"."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpWhereInputColumn["+table.tableName+"]["+ct.columnName+"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			out.append( "\t\t" + "ResultSet rs = prestmt.executeQuery() ;\n" );
		} else {
			out.append( "\t" + "public static int " + methodName.toString() + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "Statement stmt = conn.createStatement() ;\n" );
			out.append( "\t\t" + "ResultSet rs = stmt.executeQuery(\""+sql+"\") ;\n" );
		}
		out.append( "\t\t" + "while( rs.next() ) {\n" );
		for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
			out.append( "\t\t\t" + ct.table.javaClassName + " "+ct.table.javaObjectName+" = new "+ct.table.javaClassName+"() ;\n" );
		}
		if( parser.selectColumnTokenList.size() > 0 ) {
			int	columnIndex = 0 ;
			for( SqlActionSelectColumnToken ct : parser.selectColumnTokenList ) {
				columnIndex++;
				if( ct.columnName.equalsIgnoreCase(SELECT_COUNT___) ) {
					out.append("\t\t\t").append(ct.table.javaObjectName+"."+COUNT___).append(" = rs.getInt( "+columnIndex+" ) ;\n" );
				} else {
					nret = SqlActionColumn.dumpSelectOutputColumn( columnIndex, ct.column, ct.table.javaObjectName+"."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpSelectOutputColumn["+table.tableName+"]["+ct.columnName+"] failed["+nret+"]" );
						return nret;
					}
				}
			}
		}
		for( SqlActionFromTableToken ct : parser.fromTableTokenList ) {
			out.append( "\t\t\t" + ct.table.javaObjectName+"ListForSelectOutput.add("+ct.table.javaObjectName+") ;\n" );
		}
		out.append( "\t\t" + "}\n" );
		out.append( "\t\t" + "return "+parser.fromTableTokenList.get(0).table.javaObjectName+"ListForSelectOutput.size();\n" );
		out.append( "\t" + "}\n" );
		
		return 0;
	}
	
	public static int insertSqlDumpGencode( DbServerConf dbserverConf, SqlActionConf sqlactionConf, SqlActionTableConf sqlactionTableConf, String sql, String methodName, SqlActionSyntaxParser parser, SqlActionDatabase database, SqlActionTable table, StringBuilder out ) {
		
		StringBuilder		sqlBuilder = new StringBuilder() ;
		StringBuilder		methodParameters = new StringBuilder() ;
		int					columnIndex ;
		int					nret = 0 ;
		
		sqlBuilder.append( sql + " (" );
		
		columnIndex = 0 ;
		for( SqlActionColumn c : table.columnList ) {
			if( c.isAutoIncrement == false ) {
				columnIndex++;
				if( columnIndex > 1 )
					sqlBuilder.append( "," );
				sqlBuilder.append( c.columnName );
			}
		}
		
		sqlBuilder.append( ") VALUES (" );
		
		columnIndex = 0 ;
		for( SqlActionColumn c : table.columnList ) {
			if( c.isAutoIncrement == false ) {
				columnIndex++;
				if( columnIndex > 1 )
					sqlBuilder.append( "," );
				sqlBuilder.append( "?" );
			}
		}
		
		sqlBuilder.append( ")" );
		
		sql = sqlBuilder.toString() ;
		
		methodParameters.append( "Connection conn, " + table.javaClassName + " " + table.javaObjectName );
		
		out.append( "\n" );
		out.append( "\t" + "// "+sql+"\n" );
		out.append( "\t" + "public static int " + methodName + "( "+methodParameters.toString()+" ) throws Exception {\n" );
		out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
		columnIndex = 0 ;
		for( SqlActionColumn c : table.columnList ) {
			if( c.isAutoIncrement == false ) {
				columnIndex++;
				nret = SqlActionColumn.dumpWhereInputColumn( columnIndex, c, table.javaObjectName+"."+c.javaPropertyName, out ) ;
				if( nret != 0 ) {
					System.out.println( "DumpWhereInputColumn["+table.tableName+"]["+c.columnName+"] failed["+nret+"]" );
					return nret;
				}
			}
		}
		out.append( "\t\t" + "return prestmt.executeUpdate() ;\n" );
		out.append( "\t" + "}\n" );
		
		return 0;
	}
	
	public static int updateSqlDumpGencode( DbServerConf dbserverConf, SqlActionConf sqlactionConf, SqlActionTableConf sqlactionTableConf, String sql, String methodName, SqlActionSyntaxParser parser, SqlActionDatabase database, SqlActionTable table, StringBuilder out ) {
		
		StringBuilder		methodParameters = new StringBuilder() ;
		int					nret = 0 ;
		
		if( parser.whereColumnTokenList.size() > 0 ) {
			methodParameters.append( "Connection conn, " + table.javaClassName + " " + table.javaObjectName + "ForSetInput, " + table.javaClassName + " " + table.javaObjectName + "ForWhereInput" );
		} else {
			methodParameters.append( "Connection conn, " + table.javaClassName + " " + table.javaObjectName + "ForSetInput " );
		}
		
		out.append( "\n" );
		out.append( "\t" + "// "+sql+"\n" );
		if( parser.whereColumnTokenList.size() > 0 ) {
			out.append( "\t" + "public static int " + methodName + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
			int	columnIndex = 0 ;
			for( SqlActionSetColumnToken ct : parser.setColumnTokenList ) {
				columnIndex++;
				if( ct.columnValue.equals("?") ) {
					nret = SqlActionColumn.dumpSetInputColumn( columnIndex, ct.column, table.javaObjectName+"ForSetInput."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpSetInputColumn[\"+table.tableName+\"][\"+ct.columnName+\"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
				columnIndex++;
				if( ct.columnName2.equals("?") ) {
					nret = SqlActionColumn.dumpWhereInputColumn( columnIndex, ct.column, table.javaObjectName+"ForWhereInput."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpWhereInputColumn[\"+table.tableName+\"][\"+ct.columnName+\"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			out.append( "\t\t" + "return prestmt.executeUpdate() ;\n" );
		} else {
			out.append( "\t" + "public static int " + methodName + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
			int	columnIndex = 0 ;
			for( SqlActionSetColumnToken ct : parser.setColumnTokenList ) {
				columnIndex++;
				if( ct.columnValue.equals("?") ) {
					nret = SqlActionColumn.dumpSetInputColumn( columnIndex, ct.column, table.javaObjectName+"ForSetInput."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpSetInputColumn[\"+table.tableName+\"][\"+ct.columnName+\"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			out.append( "\t\t" + "return prestmt.executeUpdate() ;\n" );
		}
		out.append( "\t" + "}\n" );
		
		return 0;
	}
	
	public static int deleteSqlDumpGencode( DbServerConf dbserverConf, SqlActionConf sqlactionConf, SqlActionTableConf sqlactionTableConf, String sql, String methodName, SqlActionSyntaxParser parser, SqlActionDatabase database, SqlActionTable table, StringBuilder out ) {
		
		StringBuilder		methodParameters = new StringBuilder() ;
		int					nret = 0 ;
		
		if( parser.whereColumnTokenList.size() > 0 ) {
			methodParameters.append( "Connection conn, " + table.javaClassName + " " + table.javaObjectName + "ForWhereInput" );
		} else {
			methodParameters.append( "Connection conn" );
		}
		
		out.append( "\n" );
		out.append( "\t" + "// "+sql+"\n" );
		if( parser.whereColumnTokenList.size() > 0 ) {
			out.append( "\t" + "public static int " + methodName + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
			int	columnIndex = 0 ;
			for( SqlActionWhereColumnToken ct : parser.whereColumnTokenList ) {
				columnIndex++;
				if( ct.columnName2.equals("?") ) {
					nret = SqlActionColumn.dumpWhereInputColumn( columnIndex, ct.column, table.javaObjectName+"ForWhereInput."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpWhereInputColumn[\"+table.tableName+\"][\"+ct.columnName+\"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			out.append( "\t\t" + "return prestmt.executeUpdate() ;\n" );
		} else {
			out.append( "\t" + "public static int " + methodName.toString() + "( "+methodParameters.toString()+" ) throws Exception {\n" );
			out.append( "\t\t" + "PreparedStatement prestmt = conn.prepareStatement(\""+sql+"\") ;\n" );
			int	columnIndex = 0 ;
			for( SqlActionSetColumnToken ct : parser.setColumnTokenList ) {
				columnIndex++;
				if( ct.columnValue.equals("?") ) {
					nret = SqlActionColumn.dumpSetInputColumn( columnIndex, ct.column, table.javaObjectName+"ForSetInput."+ct.column.javaPropertyName, out ) ;
					if( nret != 0 ) {
						System.out.println( "DumpWhereInputColumn[\"+table.tableName+\"][\"+ct.columnName+\"] failed["+nret+"]" );
						return nret;
					}
				}
			}
			out.append( "\t\t" + "return prestmt.executeUpdate() ;\n" );
		}
		out.append( "\t" + "}\n" );
		
		return 0;
	}
	
}
