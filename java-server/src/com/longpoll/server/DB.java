package com.longpoll.server;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;

import java.io.File;

/**
 * acm 12/4/12 8:30 PM
 */
public class DB {

    private static DB instance;

    private ServerConfig serverConfig = null;
    private EbeanServer ebeanServer = null;

    private DB() {
    }

    public static DB instance() {
        if (instance == null) {
            instance = new DB();
        }
        return instance;
    }

    public void connectToDB() {
        if (ebeanServer == null) {
            // programmatically build a EbeanServer instance
            // specify the configuration...

            serverConfig = new ServerConfig();
            serverConfig.setName("pgtest");

            // Define DataSource parameters
            DataSourceConfig postgresDb = new DataSourceConfig();
            postgresDb.setDriver("org.postgresql.Driver");
            postgresDb.setUsername("postgres");
            postgresDb.setPassword("123456");
            postgresDb.setUrl("jdbc:postgresql://127.0.0.1:5432/chat");
            postgresDb.setHeartbeatSql("select count(*) from heart_beat");

            serverConfig.setDataSourceConfig(postgresDb);

            File ebeansResourceFile = new File("/tmp/ebeans");
            if (!ebeansResourceFile.exists()) {
                ebeansResourceFile.mkdirs();
            }
            serverConfig.setResourceDirectory("/tmp/ebeans");

            // set DDL options...
            serverConfig.setDdlGenerate(true);
            serverConfig.setDdlRun(false);

            serverConfig.setDefaultServer(true);
            serverConfig.setRegister(true);


            // automatically determine the DatabasePlatform
            // using the jdbc driver
            //serverConfig.setDatabasePlatform(new PostgresPlatform());

            // specify the entity classes (and listeners etc)
            // ... if these are not specified Ebean will search
            // ... the classpath looking for entity classes.
            serverConfig.addClass(ChatMessage.class);
            serverConfig.addClass(ChatUser.class);

            // create the EbeanServer instance
            ebeanServer = EbeanServerFactory.create(serverConfig);

            /*
                                                                                              //select u from CampusUser u
            DefaultRelationalQuery defaultRelationalQuery = new DefaultRelationalQuery(server, "select u from campus_user u");
            List<SqlRow> sqlRows = defaultRelationalQuery.findList();
            System.out.println("sqlRows: "+sqlRows);




            List<ParamMap> resultMap = convert(sqlRows);
            System.out.println("resultMap: "+resultMap);

            XStream xstream = new XStream(new StaxDriver());
            xstream.autodetectAnnotations(true);
            xstream.alias("entry", List.class);

            String xml = xstream.toXML(resultMap);
            System.out.println("xml: " + xml);

            XStream xstream2 = new XStream(new JettisonMappedXmlDriver());
            xstream2.autodetectAnnotations(true);
            xstream2.alias("entry", List.class);
            String json = xstream2.toXML(resultMap);
            System.out.println("json: " + json);

            String sql = "select count(*) as count from campus_user";
            SqlRow row = Ebean.createSqlQuery(sql).findUnique();
            System.out.println("xml: " + xstream.toXML(convert(row)));
            System.out.println("json: " + xstream2.toXML(convert(row)));
            Integer i = row.getInteger("count");
            System.out.println("Got " + i + "  - DataSource good.");

            */
        }
    }
}
