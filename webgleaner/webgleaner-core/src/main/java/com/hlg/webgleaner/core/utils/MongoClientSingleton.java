package com.hlg.webgleaner.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

/**
 * mongodb操作类，这里不采用单实例，链接不要配置太高<br>
 * mongo的异常机制不够完善，未连接也不会直接报错，<br>
 * 使用时需要多加注意
 * @author linjiexing
 * @Date 2016年3月20日
 * @version 1.1.0
 */
public class MongoClientSingleton {
	
	private static final Logger logger = LoggerFactory.getLogger(MongoClientSingleton.class);
	
	private static final MongoClientSingleton instance = new MongoClientSingleton();
	
    private com.mongodb.MongoClient mongoClient;

	private MongoClientSingleton() {
    	System.out.println("===============MongoDBUtil初始化========================");
    	CompositeConfiguration config = new CompositeConfiguration();
    	try {
            config.addConfiguration(new PropertiesConfiguration("mongo.properties"));
        } catch (ConfigurationException e) {
            logger.error(e.getMessage());
        }
        // 从配置文件中获取属性值
    	List<ServerAddress> servers = new ArrayList<ServerAddress>();
    	ServerAddress server = new ServerAddress(config.getString("mongo.host"), 
    			config.getInt("mongo.port", 27017));
    	servers.add(server);
    	
    	Builder options = new MongoClientOptions.Builder();
        options.connectionsPerHost(30);// 连接池设置为300个连接,默认为100
        options.connectTimeout(15000);// 连接超时，推荐>3000毫秒
        options.maxWaitTime(5000); //
        options.socketTimeout(0);// 套接字超时时间，0无限制
        options.threadsAllowedToBlockForConnectionMultiplier(5000);// 线程队列数，如果连接线程排满了队列就会抛出“Out of semaphores to get db”错误。
        options.writeConcern(WriteConcern.ACKNOWLEDGED);//
        
        mongoClient = new com.mongodb.MongoClient(servers, options.build());
    }
	
	public static MongoClientSingleton getInstance() {
		return instance;
	}

    // ------------------------------------共用方法---------------------------------------------------
    /**
     * 获取DB实例 - 指定DB
     * 
     * @param dbName
     * @return
     */
    public MongoDatabase getDB(String dbName) {
        if (StringUtils.isNotBlank(dbName)) {
            MongoDatabase database = mongoClient.getDatabase(dbName);
            return database;
        }
        return null;
    }
    
    /**
     * 获取collection对象 - 指定Collection
     * 
     * @param collName
     * @return
     */
    public MongoCollection<Document> getCollection(String dbName, String collName) {
        if (StringUtils.isBlank(collName) || StringUtils.isBlank(dbName)) {
            return null;
        }
        MongoCollection<Document> collection = null;
        MongoDatabase db = getDB(dbName);
        if (null != db) {
        	collection = db.getCollection(collName);
        }
        return collection;
    }

    /**
     * 插入一条数据
     * @param coll
     * @param doc
     */
    public void insertDoc(MongoCollection<Document> coll, Document doc) {
    	coll.insertOne(doc);
    }
    
//    public void insertOrUpdate(MongoCollection<Document> coll, Document doc) {
//    	coll.
//    }
    
    /**
     * 批量插入
     * @param coll
     * @param docs
     */
    public void insertMany(MongoCollection<Document> coll, List<Document> docs) {
    	coll.insertMany(docs); //会自动检验是否重复
    	//coll.insertMany(docs, new InsertManyOptions().bypassDocumentValidation(t));
    }
    
    /**
     * 查找对象 - 根据主键_id
     * 
     * @param collection
     * @param id
     * @return
     */
    public Document findById(MongoCollection<Document> coll, String id) {
        ObjectId _idobj = null;
        try {
            _idobj = new ObjectId(id);//TODO
        } catch (Exception e) {
            return null;
        }
        Document myDoc = coll.find(Filters.eq("_id", _idobj)).first();
        return myDoc;
    }

    /** 条件查询 */
    public MongoCursor<Document> find(MongoCollection<Document> coll, Bson filter) {
        return coll.find(filter).iterator();
    }
    
    /**
     * @param coll
     * @param filter
     * @return
     */
    public MongoCursor<Document> find(MongoCollection<Document> coll) {
    	return coll.find().iterator();
    }
    
    /**
     * @param coll
     * @param filter
     * @return
     */
    public MongoCursor<Document> findWithSort(MongoCollection<Document> coll, Bson sort) {
    	return coll.find().sort(sort).iterator();
    }

    /**
     * 找到并修改，原子性操作
     * @param coll
     * @param filter
     * @param newDoc
     * @return
     */
    public Document findOneAndModify(MongoCollection<Document> coll, Bson filter, Document newDoc) {
    	return coll.findOneAndReplace(filter, newDoc);
    }
    
    /**
     * 找到并删除，原子性操作
     * @param coll
     * @param filter
     * @param newDoc
     * @return
     */
    public Document findOneAndDelete(MongoCollection<Document> coll, Bson filter) {
    	return coll.findOneAndDelete(filter);
    }
    
    public DeleteResult deleteAll(MongoCollection<Document> coll) {
    	return coll.deleteMany(new Document());  //空过滤条件
    }
    
    public DeleteResult deleteMany(MongoCollection<Document> coll, Bson filter) {
    	return coll.deleteMany(filter);
    }
    
    /**
     * 计算总数
     * @param coll
     * @param filter
     * @return
     */
    public long countDocs(MongoCollection<Document> coll, Bson filter) {
    	return coll.count(filter);
    }

    /**
     * 关闭Mongodb
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

}
