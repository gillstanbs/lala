package com.zld.struts.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.zld.AjaxUtil;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.MongoDbUtils;
import com.zld.service.DataBaseService;
import com.zld.utils.Check;
import com.zld.utils.RequestUtil;

public class CarPicsUploadAction extends Action {
	@Autowired
	private DataBaseService daService;
	@Autowired 
	private MongoDbUtils mongoDbUtils;
	
	private Logger logger = Logger.getLogger(CarPicsUploadAction.class);
	/*
	 * 上传照牌图片
	 */
	public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest request,HttpServletResponse response) throws Exception{
		String action = RequestUtil.processParams(request, "action");
		if(action.equals("uploadpic")){
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			Long comid = RequestUtil.getLong(request, "comid", -1L);
			//type区分是入口还是出口，入口为0，出口为1
			Long type = RequestUtil.getLong(request, "type", -1L);
			if(comid==-1 || orderid == -1 || type == -1){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			String lefttop = RequestUtil.processParams(request, "lefttop");
			String rightbottom = RequestUtil.processParams(request, "rightbottom");
			String width = RequestUtil.processParams(request, "width");
			String height = RequestUtil.processParams(request, "height");
			Long preorderid = RequestUtil.getLong(request, "preorderid", -1L);//未结算的订单id
			String result = uploadCarPics2Mongodb(request, comid, orderid, lefttop, rightbottom,width,height,type);
			//把该照片作为未结算订单的出口照片
			if(preorderid != -1){
				result = uploadCarPics2Mongodb(request, comid, preorderid, lefttop, rightbottom, width, height, 1L);
			}
			AjaxUtil.ajaxOutput(response, result);
			//http://127.0.0.1/zld/carpicsup.do?action=uploadpic&comid=0&orderid=238705&type=0
		}else if(action.equals("downloadpic")){
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			//type区分是入口还是出口，入口为0，出口为1
			Long type = RequestUtil.getLong(request, "type", -1L);
			if(orderid == -1 || type == -1){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			downloadCarPics(orderid, type,request,response);
			//http://118.192.88.90:8080/zld/carpicsup.do?action=downloadpic&comid=0&orderid=238747&type=0
		}else if(action.equals("downloadlogpic")){
			Long comid = RequestUtil.getLong(request, "comid", -1L);
			if(comid == -1){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Map map = daService.getMap("select * from com_info_tb where id = ? ", new Object[]{comid});
			if(map!=null&&map.get("chanid")!=null){
				Long chanid = (Long)map.get("chanid");
				Map LogoMap = daService.getMap("select * from logo_tb where type = ? and orgid = ?", new Object[]{0,chanid});
				if(LogoMap!=null&&LogoMap.get("url_sec")!=null){
					String logourl = LogoMap.get("url_sec")+"";
					String fname = "停车宝";
					if(LogoMap.get("name")!=null){
						fname = LogoMap.get("name")+"";
					}
					downloadLOGOPics(logourl,fname,request,response);
				}

			}
			//http://118.192.88.90:8080/zld/carpicsup.do?action=downloadpic&comid=0&orderid=238747&type=0
		}else if(action.equals("getpicbyname")){
			String filename = RequestUtil.processParams(request, "filename");
			if("".equals(filename)){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			mongoDbUtils.getPicByFileName(filename, "car_hd_pics", response);
		}
		return null;
	}

	private String uploadCarPics2Mongodb (HttpServletRequest request,Long comid,Long orderid,String lefttop,String rightbottom,String width,String height,Long type) throws Exception{
		long currentnum = RequestUtil.getLong(request, "currentnum",-1L);
		logger.error("begin upload picture....");
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put(".jpg", "image/jpeg");
	    extMap.put(".jpeg", "image/jpeg");
	    extMap.put(".png", "image/png");
	    extMap.put(".gif", "image/gif");
		request.setCharacterEncoding("UTF-8"); // 设置处理请求参数的编码格式
		DiskFileItemFactory  factory = new DiskFileItemFactory(); // 建立FileItemFactory对象
		factory.setSizeThreshold(16*4096*1024);
		ServletFileUpload upload = new ServletFileUpload(factory);
		// 分析请求，并得到上传文件的FileItem对象
		upload.setSizeMax(16*4096*1024);
		List<FileItem> items = null;
		try {
			items =upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
			return "-1";
		}
		String filename = ""; // 上传文件保存到服务器的文件名
		InputStream is = null; // 当前上传文件的InputStream对象
		// 循环处理上传文件
		String comId = "";
		String orderId = "";
		for (FileItem item : items){
			// 处理普通的表单域
			if (item.isFormField()){
				if(item.getFieldName().equals("comid")){
					if(!item.getString().equals(""))
						comId = item.getString("UTF-8");
				}else if(item.getFieldName().equals("orderid")){
					if(!item.getString().equals("")){
						orderId = item.getString("UTF-8");
					}
				}
				
			}else if (item.getName() != null && !item.getName().equals("")){// 处理上传文件
				// 从客户端发送过来的上传文件路径中截取文件名
				logger.error(item.getName());
				filename = item.getName().substring(
						item.getName().lastIndexOf("\\")+1);
				is = item.getInputStream(); // 得到上传文件的InputStream对象
				
			}
		}
		if(comid==null&&(comId.equals("")||!Check.isLong(comId)))
			return "-1";
		if(orderid == null && (orderId.equals("") || !Check.isLong(orderId))){
			return "-1";
		}
		String file_ext =filename.substring(filename.lastIndexOf(".")).toLowerCase();// 扩展名
		String picurl = comid + "_" +orderid+ "_" + System.currentTimeMillis()/1000 + file_ext;
		BufferedInputStream in = null;  
		ByteArrayOutputStream byteout =null;
	    try {
	    	in = new BufferedInputStream(is);   
	    	byteout = new ByteArrayOutputStream(1024);        	       
		      
	 	    byte[] temp = new byte[1024];        
	 	    int bytesize = 0;        
	 	    while ((bytesize = in.read(temp)) != -1) {        
	 	          byteout.write(temp, 0, bytesize);        
	 	    }        
	 	      
	 	    byte[] content = byteout.toByteArray(); 
	 	    DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
		    mydb.requestStart();
		   // DBCollection collection = mydb.getCollection("car_pics");  
		    DBCollection collection = mydb.getCollection("car_hd_pics");
		  //  DBCollection collection = mydb.getCollection("records_test");
		    logger.error("mongodb>>>>>>>>>>>存入 car_hd_pics表");   
			BasicDBObject document = new BasicDBObject();
			document.put("comid",  comid);
			document.put("orderid", orderid);
			document.put("gate", type);
			document.put("ctime",  System.currentTimeMillis()/1000);
			document.put("type", extMap.get(file_ext));
			document.put("content", content);
			document.put("filename", picurl);
			document.put("currentnum", currentnum);
			  //开始事务
			mydb.requestStart();
			collection.insert(document);
			  //结束事务
			mydb.requestDone();
			in.close();        
		    is.close();
		    byteout.close();
		    List<Object> params = new ArrayList<Object>();
		    params.add(orderid);
		    params.add(type);
		    String sql = "select count(*) from car_picturs_tb where orderid=? and pictype=?";
		    Long count = 0L;
		    count = daService.getCount(sql, params);
		    if(count > 0){
		    	sql = "update car_picturs_tb set create_time=?,lefttop=?,rightbottom=?,width=?,height=? where orderid=? and pictype=?";
		    	daService.update(sql, new Object[]{System.currentTimeMillis()/1000,lefttop,rightbottom,width,height,orderid,type});
		    }else{
		    	sql = "insert into car_picturs_tb(orderid,pictype,create_time,lefttop,rightbottom,width,height) values(?,?,?,?,?,?,?)";
		    	daService.update(sql, new Object[]{orderid,type,System.currentTimeMillis()/1000,lefttop,rightbottom,width,height});
		    }
		} catch (Exception e) {
			e.printStackTrace();
			return "-1";
		}finally{
			if(in!=null)
				in.close();
			if(byteout!=null)
				byteout.close();
			if(is!=null)
				is.close();
		}
	  
		return "1";
	}
	
	private void downloadCarPics (Long orderid,Long type,HttpServletRequest request,HttpServletResponse response) throws Exception{
		logger.error("download from mongodb....");
		System.err.println("downloadCarPics from mongodb file:orderid="+orderid+"type="+type);
		if(orderid!=null && type !=null){
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");//
			DBCollection collection = db.getCollection("car_hd_pics");
			BasicDBObject document = new BasicDBObject();
			document.put("orderid", orderid);
			document.put("gate", type);
			//按生成时间查最近的数据
			DBObject obj  = collection.findOne(document);
			if(obj==null){
				collection = db.getCollection("car_pics");
				obj  = collection.findOne(document);
				logger.error("mongodb>>>>>>>>>>>car_hd_pics表中没有，从car_pics表中查询"+obj);
			}
			if(obj == null){
				AjaxUtil.ajaxOutput(response, "");
				logger.error("取图片错误.....");
				return;
			}
			byte[] content = (byte[])obj.get("content");
			logger.error("取图片成功.....大小:"+content.length);
			db.requestDone();
			response.setDateHeader("Expires", System.currentTimeMillis()+12*60*60*1000);
			response.setContentLength(content.length);
			response.setContentType("image/jpeg");
		    OutputStream o = response.getOutputStream();
		    o.write(content);
		    o.flush();
		    o.close();
		    System.out.println("mongdb over.....");
		}else {
			response.sendRedirect("http://sysimages.tq.cn/images/webchat_101001/common/kefu.png");
		}
	}
	private void downloadLOGOPics (String url,String fname,HttpServletRequest request,HttpServletResponse response) throws Exception{
		logger.error("downloadLOGOPics from mongodb....");
		System.err.println("downloadlogoPics from mongodb file:url="+url);
		if(url!=null){
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");//
			DBCollection collection = db.getCollection("logo_pics");
			BasicDBObject document = new BasicDBObject();
			document.put("filename", url);
			//按生成时间查最近的数据
			DBObject obj  = collection.findOne(document);
			if(obj == null){
				AjaxUtil.ajaxOutput(response, "");
				logger.error("取图片错误.....");
				return;
			}
			byte[] content = (byte[])obj.get("content");
			logger.error("取图片成功.....大小:"+content.length);
			db.requestDone();
			response.setDateHeader("Expires", System.currentTimeMillis()+12*60*60*1000);
			response.setContentLength(content.length);
			response.setContentType("image/jpeg");
			response.setHeader("Content-disposition", "attachment; filename="
					+ URLEncoder.encode(fname, "UTF-8") + ".jpg");
			
			System.out.println(fname+","+URLEncoder.encode(fname, "UTF-8"));
		    OutputStream o = response.getOutputStream();
		    o.write(content);
		    o.flush();
		    o.close();
		    System.out.println("mongdb over.....");
		}else {
//			response.sendRedirect("http://sysimages.tq.cn/images/webchat_101001/common/kefu.png");
		}
	}
}
