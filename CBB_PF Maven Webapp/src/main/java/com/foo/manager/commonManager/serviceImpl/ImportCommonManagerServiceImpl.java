package com.foo.manager.commonManager.serviceImpl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.foo.IService.IImportCommonManagerService;
import com.foo.common.CommonDefine;
import com.foo.common.CommonException;
import com.foo.common.MessageCodeDefine;
import com.foo.manager.commonManager.service.CommonManagerService;
import com.foo.util.CommonUtil;
import com.foo.util.XmlUtil;

@Service
public class ImportCommonManagerServiceImpl extends CommonManagerService implements IImportCommonManagerService{

	@Override
	public Map<String, Object> getAllDeliveries(Map<String, Object> params)
			throws CommonException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		int total = 0;

		// 开始
		Integer start = params.get("start") == null ? null : Integer
				.valueOf(params.get("start").toString());
		// 结束
		Integer limit = params.get("limit") == null ? null : Integer
				.valueOf(params.get("limit").toString());

		params.remove("start");
		params.remove("limit");
		try {
			List<String> keys=new ArrayList<String>(params.keySet());
			List<Object> values=new ArrayList<Object>(params.values());
			
			if(!params.containsKey("Fuzzy")){
				rows = commonManagerMapper.selectTableListByNVList(T_IMPORT_DELIVERY, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList(T_IMPORT_DELIVERY,
						keys,values);
			}else{
				//模糊查询
				params.remove("Fuzzy");
				keys=new ArrayList<String>(params.keySet());
				values=new ArrayList<Object>(params.values());
				rows = commonManagerMapper.selectTableListByNVList_Fuzzy(T_IMPORT_DELIVERY, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList_Fuzzy(T_IMPORT_DELIVERY,
						keys,values);
			}

//			rows = commonManagerMapper.selectTableListByNVList(T_IMPORT_SKU, 
//					keys,values,start, limit);
//
//			total = commonManagerMapper.selectTableListCountByNVList(T_IMPORT_SKU,
//					keys,values);

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("total", total);
			result.put("rows", rows);
			return result;
		} catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}

	@Override
	public void delDelivery(Map<String, Object> params) throws CommonException {
		try {
			//删除数据
			commonManagerMapper.delTableById(T_IMPORT_DELIVERY, "DELIVERY_ID",
					params.get("DELIVERY_ID"));
			
		} catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}

	@Override
	public void setDelivery(Map<String, Object> delivery, boolean statusOnly)
			throws CommonException {
		try {
			String tableName=T_IMPORT_DELIVERY;
			List<String> uniqueCol = new ArrayList<String>();
			uniqueCol.add("DELIVERY_NO");
			List<Object> uniqueVal = new ArrayList<Object>();
			uniqueVal.add(delivery.get("DELIVERY_NO"));
			String primaryCol="DELIVERY_ID";
			// 货号/业务类型唯一性校验
			//uniqueCheck(tableName,uniqueCol,uniqueVal,primaryCol,delivery.get(primaryCol),false);
			
			delivery.remove("editType");

			//更新数据
			commonManagerMapper.updateTableByNVList(tableName, primaryCol,
					delivery.get(primaryCol), new ArrayList<String>(delivery.keySet()),
					new ArrayList<Object>(delivery.values()));
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}

	@Override
	public void addDelivery(Map<String, Object> delivery) throws CommonException {
		try {
			String tableName=T_IMPORT_DELIVERY;
			List<String> uniqueCol = new ArrayList<String>();
			uniqueCol.add("DELIVERY_NO");
			List<Object> uniqueVal = new ArrayList<Object>();
			uniqueVal.add(delivery.get("DELIVERY_NO"));
			
			String primaryCol="DELIVERY_ID";
			// 货号/业务类型唯一性校验
			uniqueCheck(tableName,uniqueCol,uniqueVal,null,null,false);
			
			delivery.remove("editType");
			// 设置空id
			delivery.put(primaryCol, null);
			// 设置guid
			delivery.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB_DEFAULT,"",""));
			// 设置创建时间
			delivery.put("CREAT_TIME", new Date());

			Map primary=new HashMap();
			primary.put("primaryId", null);

			//插入数据
			commonManagerMapper.insertTableByNVList(tableName,
					new ArrayList<String>(delivery.keySet()), 
					new ArrayList<Object>(delivery.values()),
					primary);
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void batchSubmit_SKU(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");
		List<Map<String,Object>> skuList = null;
		Map sku = null;
		String tableName=T_IMPORT_SKU;
		String primaryCol="SKU_ID";
		String uniqueCol="";
		StringBuilder errorMessage = new StringBuilder("");
		for(String guid:guidList){
			try{
				//获取商品信息
				skuList = commonManagerMapper.selectTableListByCol(tableName, "GUID", guid, null, null);
				if(skuList !=null && skuList.size() == 1){
					sku = skuList.get(0);
				}else{
					errorMessage.append(guid+"：商品数据不存在！;<br/>");
					continue;
				}
				//数据完备性检查
				if(sku.get(primaryCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+primaryCol+"为空！）;<br/>");
					continue;
				}
				// 唯一性校验
				uniqueCol="ITEM_NO";
				//数据完备性检查
				if(sku.get(uniqueCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+uniqueCol+"为空！）;<br/>");
					continue;
				}
				uniqueCheck(tableName,uniqueCol,sku.get(uniqueCol),primaryCol,sku.get(primaryCol),false);
	
				Map data =  importCommonManagerMapper.selectDataForMessage201_import(guid);

				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				//测试
	//			String reponse = "";
				
				String reponse = submitXml_SKU(guid,data,CommonDefine.CEB201,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					sku.put("APP_TIME", currentTime);
					sku.put("PRE_NO", reponse);
					
					importCommonManagerMapper.updateSku_import(sku);
				}else{
					errorMessage.append(guid+"：提交错误（"+reponse+"）;<br/>");
				}
			}catch(CommonException e){
				errorMessage.append(guid+"：提交错误（"+e.getErrorMessage()+"）;<br/>");
			}
		
		}
		//判断是否有错误信息，如果有抛出异常
		if(!errorMessage.toString().isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, errorMessage.toString());
		}
	}
	
	@Override
	public Map<String, Object> getAllOrders(Map<String, Object> params)
			throws CommonException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		int total = 0;

		// 开始
		Integer start = params.get("start") == null ? null : Integer
				.valueOf(params.get("start").toString());
		// 结束
		Integer limit = params.get("limit") == null ? null : Integer
				.valueOf(params.get("limit").toString());

		params.remove("start");
		params.remove("limit");
		try {
			String tableName = T_IMPORT_ORDERS;
			if(params.get("IN_USE_LOGISTICS")!=null){
				if(Boolean.FALSE.equals(params.get("IN_USE_LOGISTICS"))){
					tableName = V_IMPORT_ORDERS_UNUSE;
				}
				params.remove("IN_USE_LOGISTICS");
			}
//			if(params.get("IN_USE_PAY")!=null){
//				if(Boolean.FALSE.equals(params.get("IN_USE_PAY"))){
//					tableName = V_IMPORT_ORDERS_UNUSE_PAY;
//				}
//				params.remove("IN_USE_PAY");
//			}
			List<String> keys=new ArrayList<String>(params.keySet());
			List<Object> values=new ArrayList<Object>(params.values());
			
			if(!params.containsKey("Fuzzy")){
				rows = commonManagerMapper.selectTableListByNVList(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList(tableName,
						keys,values);
			}else{
				//模糊查询
				params.remove("Fuzzy");
				keys=new ArrayList<String>(params.keySet());
				values=new ArrayList<Object>(params.values());
				rows = commonManagerMapper.selectTableListByNVList_Fuzzy(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList_Fuzzy(tableName,
						keys,values);
			}

//			rows = commonManagerMapper.selectTableListByNVList(tableName, 
//					keys,values,start, limit);
//			total = commonManagerMapper.selectTableListCountByNVList(tableName,
//					keys,values);
			
//			for(Map<String, Object> row:rows){
//				row.put("GOODSList", getGoodsList(row));
//			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("total", total);
			result.put("rows", rows);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void delOrder(Map<String, Object> params) throws CommonException {
		try {
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					params.get("APP_STATUS"))) {
				String guid = params.get("GUID").toString();
				//
				Map data =  importCommonManagerMapper.selectDataForMessage303_import(guid);
				
				if(data.get("APP_TIME")!=null){
					data.remove("APP_TIME");
					
					//更新申报时间
					String currentTime = new SimpleDateFormat(
							CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
					
					String reponse = submitXml_ORDER(guid,data,null,CommonDefine.CEB303,currentTime);
					
					if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse)){
						//删除数据
						commonManagerMapper.delTableById(T_IMPORT_ORDERS, "ORDERS_ID",
								params.get("ORDERS_ID"));
					}else{
						//抛出错误信息
						throw new CommonException(new Exception(),
								MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
					}
				}
			}else if(Integer.valueOf(CommonDefine.APP_STATUS_STORE).equals(
					params.get("APP_STATUS"))){
				//删除数据
				commonManagerMapper.delTableById(T_IMPORT_ORDERS, "ORDERS_ID",
						params.get("ORDERS_ID"));
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	
	@Override
	public void batchSubmit_ORDER(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");
		List<Map<String,Object>> orderList = null;
		Map order = null;
		String tableName=T_IMPORT_ORDERS;
		String primaryCol="ORDERS_ID";
		String uniqueCol="";
		StringBuilder errorMessage = new StringBuilder("");
		for(String guid:guidList){
			try{
				//获取运单信息
				orderList = commonManagerMapper.selectTableListByCol(tableName, "GUID", guid, null, null);
				if(orderList !=null && orderList.size() == 1){
					order = orderList.get(0);
				}else{
					errorMessage.append(guid+"：订单数据不存在！;<br/>");
					continue;
				}
				//数据完备性检查
				if(order.get(primaryCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+primaryCol+"为空！）;<br/>");
					continue;
				}
				// 唯一性校验
				uniqueCol="ORDER_NO";
				//数据完备性检查
				if(order.get(uniqueCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+uniqueCol+"为空！）;<br/>");
					continue;
				}
				uniqueCheck(tableName,uniqueCol,order.get(uniqueCol),primaryCol,order.get(primaryCol),false);

				Map data =  importCommonManagerMapper.selectDataForMessage301_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage301_import(Integer.valueOf(order.get("ORDERS_ID").toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				//测试
	//			String reponse = "";
				
				String reponse = submitXml_ORDER(guid,data,subDataList,CommonDefine.CEB301,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					order.put("APP_TIME", currentTime);
//					order.put("PRE_NO", reponse);
					order.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
					order.put("RETURN_STATUS",CommonDefine.RETURN_STATUS_2);
					
					importCommonManagerMapper.updateOrder_import(order);
				}else{
					errorMessage.append(guid+"：提交错误（"+reponse+"）;<br/>");
				}
			}catch(CommonException e){
				errorMessage.append(guid+"：提交错误（"+e.getErrorMessage()+"）;<br/>");
			}
		
		}
		//判断是否有错误信息，如果有抛出异常
		if(!errorMessage.toString().isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, errorMessage.toString());
		}
	}
	
	@Override
	public void addOrder(Map<String, Object> order) throws CommonException {
		try {
			String tableName=T_IMPORT_ORDERS;
			String uniqueCol="ORDER_NO";
			String primaryCol="ORDERS_ID";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,order.get(uniqueCol),null,null,false);
			
			order.remove("editType");
			// 设置空id
			order.put(primaryCol, null);
			
			// 设置guid
			order.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB301,order.get("EBC_CODE").toString(),order.get("CUSTOM_CODE").toString()));
			// 设置创建时间
			order.put("CREAT_TIME", new Date());
			
			Object orderNo = order.get("ORDER_NO");
			List<Map> GOODSList=(List<Map>)order.get("GOODSList");
			order.remove("GOODSList");
			//插入数据
			Map primary=new HashMap();
			primary.put("primaryId", null);
			
			if(order.get("UNDER_THE_SINGER_ID") == null || order.get("UNDER_THE_SINGER_ID").toString().isEmpty()){
				order.put("UNDER_THE_SINGER_ID", null);
			}
			if(order.get("TAX_FEE") == null || order.get("TAX_FEE") .toString().isEmpty()){
				order.put("TAX_FEE", null);
			}
			
			commonManagerMapper.insertTableByNVList(tableName,
					new ArrayList<String>(order.keySet()), 
					new ArrayList<Object>(order.values()),
					primary);
			Object primaryId=primary.get("primaryId");
			setGoodsList(GOODSList,orderNo,primaryId);
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					order.get("APP_STATUS"))) {
				String guid = order.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage301_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage301_import(Integer.valueOf(primaryId.toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_ORDER(guid,data,subDataList,CommonDefine.CEB301,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					order.put("APP_TIME", currentTime);
//					order.put("PRE_NO", reponse);
					order.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
					order.put("RETURN_STATUS",CommonDefine.RETURN_STATUS_2);
					
					importCommonManagerMapper.updateOrder_import(order);
				}else{
					//删除数据
					commonManagerMapper.delTableById(T_IMPORT_ORDERS, "GUID",
							guid);
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void setOrder(Map<String, Object> order, boolean statusOnly)
			throws CommonException {
		try {
			String tableName=T_IMPORT_ORDERS;
			String uniqueCol="ORDER_NO";
			String primaryCol="ORDERS_ID";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,order.get(uniqueCol),primaryCol,order.get(primaryCol),false);
			
			order.remove("editType");
			
			Object orderNo = order.get("ORDER_NO");
			List<Map> GOODSList=(List<Map>)order.get("GOODSList");
			order.remove("GOODSList");
			
			Object primaryId=order.get(primaryCol);
			
			if(order.get("UNDER_THE_SINGER_ID") == null || order.get("UNDER_THE_SINGER_ID").toString().isEmpty()){
				order.put("UNDER_THE_SINGER_ID", null);
			}

			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					order.get("APP_STATUS"))) {
				String guid = order.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage301_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage301_import(Integer.valueOf(primaryId.toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_ORDER(guid,data,subDataList,CommonDefine.CEB301,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					order.put("APP_TIME", currentTime);
//					order.put("PRE_NO", reponse);
					order.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
					order.put("RETURN_STATUS",CommonDefine.RETURN_STATUS_2);
					//更新数据
					if(order.get("TAX_FEE") == null || order.get("TAX_FEE") .toString().isEmpty()){
						order.put("TAX_FEE", null);
					}
					
					
					commonManagerMapper.updateTableByNVList(tableName, primaryCol,
							order.get(primaryCol), new ArrayList<String>(order.keySet()),
							new ArrayList<Object>(order.values()));
//					setGoodsList(GOODSList,orderNo,primaryId);
					
//					importCommonManagerMapper.updateOrder_import(order);
				}else{
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}else{
				//更新数据
				if(order.get("TAX_FEE") == null || order.get("TAX_FEE") .toString().isEmpty()){
					order.put("TAX_FEE", null);
				}
				System.out.println(order);
				commonManagerMapper.updateTableByNVList(tableName, primaryCol,
						order.get(primaryCol), new ArrayList<String>(order.keySet()),
						new ArrayList<Object>(order.values()));
//				setGoodsList(GOODSList,orderNo,primaryId);
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	public void setGoodsList(List<Map> GOODSList,Object orderNo, Object ordersId){
		commonManagerMapper.delTableById(T_IMPORT_ORDER_DETAIL, "ORDERS_ID", ordersId);
		Date createTime=new Date();
		for(int i=0;i<GOODSList.size();i++){
			GOODSList.get(i).put("PRICE", GOODSList.get(i).get("ONE_PRICE"));
			GOODSList.get(i).put("NOTE", GOODSList.get(i).get("NOTE_OD"));
			Map good=new HashMap();
			good.put("ORDERS_ID", ordersId);
			good.put("ORDER_NO", orderNo);
			good.put("GNUM", i+1);
			good.put("CREAT_TIME", createTime);
			String[] copyColumns=new String[]{"ITEM_NO","QTY","PRICE","TOTAL","NOTE"};
			for(String col:copyColumns){
				good.put(col, GOODSList.get(i).get(col));
			}
			Map primary=new HashMap();
			primary.put("primaryId", null);
			commonManagerMapper.insertTableByNVList(T_IMPORT_ORDER_DETAIL,
					new ArrayList<String>(good.keySet()), 
					new ArrayList<Object>(good.values()),
					primary);
		}
	}
	
	public List<Map<String, Object>> getGoodsList(Map<String, Object> order){
		List<Map<String, Object>> goods=commonManagerMapper.selectTableListByCol(T_IMPORT_ORDER_DETAIL, "ORDERS_ID", order.get("ORDERS_ID"), null, null);
		for(Map<String, Object> good:goods){
			good.put("ONE_PRICE", good.get("PRICE"));
			good.remove("PRICE");
			good.put("NOTE_OD", good.get("NOTE"));
			good.remove("NOTE");
			List<Map<String, Object>> skus=commonManagerMapper.selectTableListByCol(T_IMPORT_SKU, "ITEM_NO", good.get("ITEM_NO"), 0, 1);
			if(!skus.isEmpty()){
				for(String col:skus.get(0).keySet()){
					if(!good.containsKey(col))
						good.put(col, skus.get(0).get(col));
				}
			}
		}
		return goods;
	}
	
	
	@Override
	public Map<String, Object> getAllLogisticses(Map<String, Object> params)
			throws CommonException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		int total = 0;

		// 开始
		Integer start = params.get("start") == null ? null : Integer
				.valueOf(params.get("start").toString());
		// 结束
		Integer limit = params.get("limit") == null ? null : Integer
				.valueOf(params.get("limit").toString());

		params.remove("start");
		params.remove("limit");
		try {
			String tableName = V_IMPORT_LOGISTICS;
			if(params.get("IN_USE")!=null){
//				if(Boolean.FALSE.equals(params.get("IN_USE"))){
//					tableName = V_IMPORT_LOGISTICS_UNUSE;
//				}
//				params.remove("IN_USE");
			}
			
			List<String> keys=new ArrayList<String>(params.keySet());
			List<Object> values=new ArrayList<Object>(params.values());
			
			if(!params.containsKey("Fuzzy")){
				rows = commonManagerMapper.selectTableListByNVList(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList(tableName,
						keys,values);
			}else{
				//模糊查询
				params.remove("Fuzzy");
				keys=new ArrayList<String>(params.keySet());
				values=new ArrayList<Object>(params.values());
				rows = commonManagerMapper.selectTableListByNVList_Fuzzy(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList_Fuzzy(tableName,
						keys,values);
			}
			
			for(Map<String, Object> row:rows){
				Map<String, Object> additionInfo=getRelOrder(row);
				if(additionInfo!=null)
					row.putAll(additionInfo);
			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("total", total);
			result.put("rows", rows);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void delLogistics(Map<String, Object> params) throws CommonException {
		try {
			commonManagerMapper.delTableById(T_IMPORT_LOGISTICS, "LOGISTICS_ID",
					params.get("LOGISTICS_ID"));
		} catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void setLogistics(Map<String, Object> logistics, boolean statusOnly)
			throws CommonException {
		
		logistics.remove("EBC_CODE");
		logistics.remove("EBP_CODE");
		
		try {
			String primaryCol="LOGISTICS_ID";
			String tableName=T_IMPORT_LOGISTICS;
			if(statusOnly){
				List<String> keys=Arrays.asList(new String[]{
						"LOGISTICS_STATUS","RETURN_STATUS","RETURN_TIME","RETURN_INFO"});
				List<Object> values=Arrays.asList(new Object[]{
						logistics.get("LOGISTICS_STATUS"),
						null,null,null});
				commonManagerMapper.updateTableByNVList(tableName, primaryCol,
						logistics.get(primaryCol), keys, values);
				Object primaryId=logistics.get(primaryCol);
				
				//生成xml报文
				//修改APP_STATUS为upload
				logistics.put("APP_STATUS", CommonDefine.APP_STATUS_UPLOAD);
				submitXml_LOGISTICS(logistics,
						Integer.valueOf(primaryId.toString()),
						CommonDefine.CEB513, CommonDefine.LOGISTICS_TYPE_IMPORT);
			}else{
				String uniqueCol="LOGISTICS_NO";
				// 唯一性校验
				uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),primaryCol,logistics.get(primaryCol),false);
				uniqueCol="ORDER_NO";
				uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),primaryCol,logistics.get(primaryCol),false);
				logistics.remove("editType");
				
				commonManagerMapper.updateTableByNVList(tableName, primaryCol,
						logistics.get(primaryCol), new ArrayList<String>(logistics.keySet()),
						new ArrayList<Object>(logistics.values()));
	
				Object primaryId=logistics.get(primaryCol);
				// 生成xml报文
				submitXml_LOGISTICS(logistics,
						Integer.valueOf(primaryId.toString()),
						CommonDefine.CEB511, CommonDefine.LOGISTICS_TYPE_IMPORT);
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
				
	
	@Override
	public void addLogistics(Map<String, Object> logistics) throws CommonException {
		try {
			String tableName=T_IMPORT_LOGISTICS;
			String uniqueCol="LOGISTICS_NO";
			String primaryCol="LOGISTICS_ID";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),null,null,false);
			uniqueCol="ORDER_NO";
			uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),null,null,false);
			
			logistics.remove("editType");
			// 设置空id
			logistics.put(primaryCol, null);
			
			// 设置guid
			logistics.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB511,"",""));
			// 设置创建时间
			logistics.put("CREAT_TIME", new Date());
			
			Map primary=new HashMap();
			primary.put("primaryId", null);
			
			logistics.remove("EBC_CODE");
			logistics.remove("EBP_CODE");

			//插入数据
			commonManagerMapper.insertTableByNVList(tableName,
					new ArrayList<String>(logistics.keySet()), 
					new ArrayList<Object>(logistics.values()),
					primary);
			
			Object primaryId=primary.get("primaryId");

			logistics.put("APP_STATUS", CommonDefine.APP_STATUS_UPLOAD);
			// 生成xml报文
			submitXml_LOGISTICS(logistics,
					Integer.valueOf(primaryId.toString()), CommonDefine.CEB511,
					CommonDefine.LOGISTICS_TYPE_IMPORT);
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	public Map<String, Object> getRelOrder(Map<String, Object> data){
		Map<String, Object> resultMap=null;
		Object ORDER_NO=data.get("ORDER_NO");
		if(ORDER_NO!=null){
			List<Map<String, Object>> orders=commonManagerMapper.selectTableListByCol(
					T_IMPORT_ORDERS, "ORDER_NO", ORDER_NO, null, null);
			if(orders!=null&&!orders.isEmpty()){
				Map<String, Object> orderMap=orders.get(0);
				resultMap=new HashMap<String, Object>();
				resultMap.put("EBP_CODE", orderMap.get("EBP_CODE"));
				resultMap.put("EBC_CODE", orderMap.get("EBC_CODE"));
				resultMap.put("AGENT_CODE", orderMap.get("AGENT_CODE"));
				resultMap.put("CONSIGNEE_COUNTRY_O", orderMap.get("CONSIGNEE_COUNTRY"));
				//resultMap.put("GOODSList", getGoodsList(orderMap));
			}
		}
		return resultMap;
	}
	
	
	@Override
	public Map<String, Object> getAllInventorys(Map<String, Object> params)
			throws CommonException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		int total = 0;

		// 开始
		Integer start = params.get("start") == null ? null : Integer
				.valueOf(params.get("start").toString());
		// 结束
		Integer limit = params.get("limit") == null ? null : Integer
				.valueOf(params.get("limit").toString());

		params.remove("start");
		params.remove("limit");
		try {
			String tableName = V_IMPORT_INVENTORY;
			/*if(params.get("IN_USE")!=null){
				if(Boolean.FALSE.equals(params.get("IN_USE"))){
					tableName = "v_logistics_unuse";
				}
				params.remove("IN_USE");
			}*/
			
			List<String> keys=new ArrayList<String>(params.keySet());
			List<Object> values=new ArrayList<Object>(params.values());
			
			if(!params.containsKey("Fuzzy")){
				rows = commonManagerMapper.selectTableListByNVList(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList(tableName,
						keys,values);
			}else{
				//模糊查询
				params.remove("Fuzzy");
				keys=new ArrayList<String>(params.keySet());
				values=new ArrayList<Object>(params.values());
				rows = commonManagerMapper.selectTableListByNVList_Fuzzy(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList_Fuzzy(tableName,
						keys,values);
			}
			
//			rows = commonManagerMapper.selectTableListByNVList(tableName, 
//					keys,values,start, limit);
//			total = commonManagerMapper.selectTableListCountByNVList(tableName,
//					keys,values);
			
			for(Map<String, Object> row:rows){
				
				//在进口时，收发货人代码自动填收货人的身份证号，收发货人名称自动填收货人姓名；
				//出口时，收发货人代码自动填物流企业代码，收发货人名称自动填物流企业名称。
				//界面上只要显示收发货人名称，不用显示身份证号或物流企业代码
				if ("I".equals(row.get("IE_FLAG").toString())) {
					row.put("OWNER_NAME", row.get("CONTACT_NAME"));
				} else if ("E".equals(row.get("IE_FLAG").toString())) {
					row.put("OWNER_NAME", row.get("CODE_NAME"));
				}
				row.put("GOODSList", 
					commonManagerMapper.selectTableListByCol(
						V_IMPORT_INVENTORY_DETAIL, "INVENTORY_ID", row.get("INVENTORY_ID"), null, null));
			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("total", total);
			result.put("rows", rows);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void delInventory(Map<String, Object> params) throws CommonException {
		
		try {
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					params.get("APP_STATUS"))) {
				String guid = params.get("GUID").toString();
				//
				Map data =  importCommonManagerMapper.selectDataForMessage602_import(guid);
				
				if(data.get("APP_TIME")!=null){
					data.remove("APP_TIME");
					
					//更新申报时间
					String currentTime = new SimpleDateFormat(
							CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
					
					String reponse = submitXml_INVENTORY(guid,data,null,CommonDefine.CEB603,currentTime);
					
					if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse)){
						//删除数据
						commonManagerMapper.delTableById(T_IMPORT_INVENTORY, "INVENTORY_ID",
								params.get("INVENTORY_ID"));
					}else{
						//抛出错误信息
						throw new CommonException(new Exception(),
								MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
					}
				}
			}else if(Integer.valueOf(CommonDefine.APP_STATUS_STORE).equals(
					params.get("APP_STATUS"))){
				//删除数据
				commonManagerMapper.delTableById(T_IMPORT_INVENTORY, "INVENTORY_ID",
						params.get("INVENTORY_ID"));
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	
	@Override
	public void batchSubmit_INVENTORY(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");
		List<Map<String,Object>> inventoryList = null;
		Map inventory = null;
		String tableName=T_IMPORT_INVENTORY;
		String primaryCol="INVENTORY_ID";
		String uniqueCol="";
		StringBuilder errorMessage = new StringBuilder("");
		for(String guid:guidList){
			try{
				//获取运单信息
				inventoryList = commonManagerMapper.selectTableListByCol(tableName, "GUID", guid, null, null);
				if(inventoryList !=null && inventoryList.size() == 1){
					inventory = inventoryList.get(0);
				}else{
					errorMessage.append(guid+"：清单数据不存在！;<br/>");
					continue;
				}
				//数据完备性检查
				if(inventory.get(primaryCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+primaryCol+"为空！）;<br/>");
					continue;
				}

				Map data =  importCommonManagerMapper.selectDataForMessage601_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage601_import(Integer.valueOf(inventory.get("INVENTORY_ID").toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				//测试
	//			String reponse = "";
				
				String reponse = submitXml_INVENTORY(guid,data,subDataList,CommonDefine.CEB601,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					inventory.put("APP_TIME", currentTime);
					inventory.put("PRE_NO", reponse);
					
					importCommonManagerMapper.updateInventory_import(inventory);
				}else{
					errorMessage.append(guid+"：提交错误（"+reponse+"）;<br/>");
				}
			}catch(CommonException e){
				errorMessage.append(guid+"：提交错误（"+e.getErrorMessage()+"）;<br/>");
			}
		
		}
		//判断是否有错误信息，如果有抛出异常
		if(!errorMessage.toString().isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, errorMessage.toString());
		}
	}
	
	@Override
	public void setInventory(Map<String, Object> inventory, boolean statusOnly)
			throws CommonException {
		try {
			String tableName=T_IMPORT_INVENTORY;
			String uniqueCol="COP_NO";
			String primaryCol="INVENTORY_ID";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,inventory.get(uniqueCol),primaryCol,inventory.get(primaryCol),false);
			uniqueCol="LOGISTICS_NO";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,inventory.get(uniqueCol),primaryCol,inventory.get(primaryCol),false);
			
			inventory.remove("editType");
			inventory.remove("EBC_CODE");
			
			List<Map> GOODSList=(List<Map>)inventory.get("GOODSList");
			inventory.remove("GOODSList");

			Object primaryId=inventory.get(primaryCol);

			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					inventory.get("APP_STATUS"))) {
				String guid = inventory.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage601_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage601_import(Integer.valueOf(primaryId.toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_INVENTORY(guid,data,subDataList,CommonDefine.CEB601,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					inventory.put("APP_TIME", currentTime);
					inventory.put("PRE_NO", reponse);
					//更新数据
					commonManagerMapper.updateTableByNVList(tableName, primaryCol,
							inventory.get(primaryCol), new ArrayList<String>(inventory.keySet()),
							new ArrayList<Object>(inventory.values()));
					setInventoryGoodsList(GOODSList,primaryId,true);
					
//					importCommonManagerMapper.updateOrder_import(order);
				}else{
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}else{
				//更新数据
				commonManagerMapper.updateTableByNVList(tableName, primaryCol,
						inventory.get(primaryCol), new ArrayList<String>(inventory.keySet()),
						new ArrayList<Object>(inventory.values()));
				setInventoryGoodsList(GOODSList,primaryId,true);
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	
	@Override
	public void addInventory(Map<String, Object> inventory) throws CommonException {
		try {
			String tableName=T_IMPORT_INVENTORY;
			String uniqueCol="COP_NO";
			String primaryCol="INVENTORY_ID";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,inventory.get(uniqueCol),null,null,false);
			uniqueCol="LOGISTICS_NO";
			// 唯一性校验
			uniqueCheck(tableName,uniqueCol,inventory.get(uniqueCol),null,null,false);
			
			inventory.remove("editType");
			// 设置空id
			inventory.put(primaryCol, null);
			
			// 设置guid
			inventory.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB601,inventory.get("EBC_CODE").toString(),inventory.get("CUSTOM_CODE").toString()));
			
			inventory.remove("EBC_CODE");
			// 设置创建时间
			inventory.put("CREAT_TIME", new Date());
			
			List<Map> GOODSList=(List<Map>)inventory.get("GOODSList");
			inventory.remove("GOODSList");
			
			Map primary=new HashMap();
			primary.put("primaryId", null);
			commonManagerMapper.insertTableByNVList(tableName,
					new ArrayList<String>(inventory.keySet()), 
					new ArrayList<Object>(inventory.values()),
					primary);
			
			Object primaryId=primary.get("primaryId");
			setInventoryGoodsList(GOODSList,primaryId,false);
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					inventory.get("APP_STATUS"))) {
				String guid = inventory.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage601_import(guid);
				
				//获取订单详细信息
				List<Map> subDataList = importCommonManagerMapper.selectSubDataForMessage601_import(Integer.valueOf(primaryId.toString()));
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_INVENTORY(guid,data,subDataList,CommonDefine.CEB601,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					inventory.put("APP_TIME", currentTime);
					inventory.put("PRE_NO", reponse);
					
					importCommonManagerMapper.updateInventory_import(inventory);
				}else{
					//删除数据
					commonManagerMapper.delTableById(T_IMPORT_INVENTORY, "GUID",
							guid);
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	public void setInventoryGoodsList(List<Map> GOODSList,Object inventoryId,boolean isUpdate){

		String tableName=T_IMPORT_INVENTORY_DETAIL;
		String primaryCol="INVENTORY_DETAIL_ID";
		String ownerCol="INVENTORY_ID";
		Date createTime=new Date();
		for(int i=0;i<GOODSList.size();i++){
			Map rowMap=GOODSList.get(i);
			rowMap.put("PRICE", rowMap.get("ONE_PRICE"));
			rowMap.put("UNIT", rowMap.get("UNIT_I"));
			rowMap.put("UNIT1", rowMap.get("UNIT1_I"));
			rowMap.put("UNIT2", rowMap.get("UNIT2_I"));
			
			Map good=new HashMap();
			good.put(ownerCol, inventoryId);
			good.put("CREAT_TIME", createTime);
			String[] copyColumns=new String[]{primaryCol,
					"GNUM","ITEM_NO","QTY","QTY1","QTY2",
					"UNIT","UNIT1","UNIT2","PRICE","TOTAL"};
			for(String col:copyColumns){
				good.put(col, rowMap.get(col));
			}

			if(!isUpdate){
				good.remove(primaryCol);
				Map primary=new HashMap();
				primary.put("primaryId", null);
				commonManagerMapper.insertTableByNVList(tableName,
						new ArrayList<String>(good.keySet()), 
						new ArrayList<Object>(good.values()),
						primary);
			}else{
				Object primaryValue=good.get(primaryCol);
				commonManagerMapper.updateTableByNVList(tableName,
						primaryCol,primaryValue,
						new ArrayList<String>(good.keySet()), 
						new ArrayList<Object>(good.values()));
			}
		}
	}
	
	
	@Override
	public Map<String, Object> getAllPayes(Map<String, Object> params)
			throws CommonException {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

		int total = 0;

		// 开始
		Integer start = params.get("start") == null ? null : Integer
				.valueOf(params.get("start").toString());
		// 结束
		Integer limit = params.get("limit") == null ? null : Integer
				.valueOf(params.get("limit").toString());

		params.remove("start");
		params.remove("limit");
		try {
			String tableName = V_IMPORT_PAY;
			
			List<String> keys=new ArrayList<String>(params.keySet());
			List<Object> values=new ArrayList<Object>(params.values());
			
			if(!params.containsKey("Fuzzy")){
				rows = commonManagerMapper.selectTableListByNVList(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList(tableName,
						keys,values);
			}else{
				//模糊查询
				params.remove("Fuzzy");
				keys=new ArrayList<String>(params.keySet());
				values=new ArrayList<Object>(params.values());
				rows = commonManagerMapper.selectTableListByNVList_Fuzzy(tableName, 
						keys,values,start, limit);
				total = commonManagerMapper.selectTableListCountByNVList_Fuzzy(tableName,
						keys,values);
			}
			
//			rows = commonManagerMapper.selectTableListByNVList(tableName, 
//					keys,values,start, limit);
//			total = commonManagerMapper.selectTableListCountByNVList(tableName,
//					keys,values);

			for(Map<String, Object> row:rows){
				Map<String, Object> additionInfo=getRelOrder(row);
				if(additionInfo!=null)
					row.putAll(additionInfo);
			}

			Map<String, Object> result = new HashMap<String, Object>();
			result.put("total", total);
			result.put("rows", rows);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	
	@Override
	public void addPay(Map<String, Object> pay) throws CommonException {
		try {
			String tableName=T_IMPORT_PAY;
			List<String> uniqueCol = new ArrayList<String>();
			uniqueCol.add("PAY_NO");
			List<Object> uniqueVal = new ArrayList<Object>();
			uniqueVal.add(pay.get("PAY_NO"));
			
			String primaryCol="PAY_ID";
			// 货号/业务类型唯一性校验
			uniqueCheck(tableName,uniqueCol,uniqueVal,null,null,false);
			
			pay.remove("editType");
			// 设置空id
			pay.put(primaryCol, null);
			// 设置guid
			pay.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB401,pay.get("EBP_CODE").toString(),pay.get("PAY_CODE").toString()));
			// 设置创建时间
			pay.put("CREAT_TIME", new Date());

			Map primary=new HashMap();
			primary.put("primaryId", null);
			
			//无用数据
			pay.remove("GOODS_VALUE");
			pay.remove("TAX_FEE");
			pay.remove("FREIGHT");
			pay.remove("CURRENCY");
			
			pay.put("PAY_TYPE", "M");
			

			//插入数据
			commonManagerMapper.insertTableByNVList(tableName,
					new ArrayList<String>(pay.keySet()), 
					new ArrayList<Object>(pay.values()),
					primary);
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					pay.get("APP_STATUS"))) {
				String guid = pay.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage401_import(guid);
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_PAY(guid,data,CommonDefine.CEB401,currentTime);
				
				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) || 
						reponse.startsWith("P")){
					
					pay.put("APP_TIME", currentTime);
					pay.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
					pay.put("RETURN_STATUS",CommonDefine.RETURN_STATUS_2);
					
					importCommonManagerMapper.updatePay_import(pay);
				}else{
					//删除数据
					commonManagerMapper.delTableById(T_IMPORT_PAY, "GUID",
							guid);
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	@Override
	public void setPay(Map<String, Object> pay)
			throws CommonException {
		try {
			String tableName=T_IMPORT_PAY;
			List<String> uniqueCol = new ArrayList<String>();
			uniqueCol.add("PAY_NO");
			List<Object> uniqueVal = new ArrayList<Object>();
			uniqueVal.add(pay.get("PAY_NO"));
			
			String primaryCol="PAY_ID";
			
			Map oldData = commonManagerMapper.selectTableById("T_IMPORT_PAY",
					"PAY_ID", Integer.valueOf(pay.get("PAY_ID").toString()));

			if (!oldData.get("PAY_NO").toString()
					.equals(pay.get("PAY_NO").toString())) {
				// 货号/业务类型唯一性校验
				uniqueCheck(tableName, uniqueCol, uniqueVal, primaryCol,
						pay.get(primaryCol), false);
			}
			pay.remove("editType");
			
			//无用数据
			pay.remove("GOODS_VALUE");
			pay.remove("TAX_FEE");
			pay.remove("FREIGHT");
			pay.remove("CURRENCY");

			pay.put("PAY_TYPE", "M");
			//生成报文
			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
					pay.get("APP_STATUS"))) {
				String guid = pay.get("GUID").toString();
				Map data =  importCommonManagerMapper.selectDataForMessage401_import(guid);
				
				//更新申报时间
				String currentTime = new SimpleDateFormat(
						CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
				
				String reponse = submitXml_PAY(guid,data,CommonDefine.CEB401,currentTime);

				if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse) ||
						reponse.startsWith("P")){
					
					pay.put("APP_TIME", currentTime);
					pay.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
					pay.put("RETURN_STATUS",CommonDefine.RETURN_STATUS_2);
					
//					importCommonManagerMapper.updateSku_import(sku);
					//更新数据
					commonManagerMapper.updateTableByNVList(tableName, primaryCol,
							pay.get(primaryCol), new ArrayList<String>(pay.keySet()),
							new ArrayList<Object>(pay.values()));
				}else{
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
				}
			}else{
				//更新数据
				commonManagerMapper.updateTableByNVList(tableName, primaryCol,
						pay.get(primaryCol), new ArrayList<String>(pay.keySet()),
						new ArrayList<Object>(pay.values()));
			}
		} catch (CommonException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}
	
	
	@Override
	public void delPay(Map<String, Object> params) throws CommonException {
		try {
			
			//删除数据
			commonManagerMapper.delTableById(T_IMPORT_PAY, "PAY_ID",
					params.get("PAY_ID"));
			
			
//			//生成报文
//			if (Integer.valueOf(CommonDefine.APP_STATUS_UPLOAD).equals(
//					params.get("APP_STATUS"))) {
//				String guid = params.get("GUID").toString();
//				//
//				Map data =  importCommonManagerMapper.selectDataForMessage502_NJ(guid);
//				
//				if(data.get("APP_TIME")!=null){
//					data.remove("APP_TIME");
//					
//					//更新申报时间
//					String currentTime = new SimpleDateFormat(
//							CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
//					
//					String reponse = submitXml_LOGISTICS(guid,data,null,CommonDefine.CEB502,currentTime);
//					
//					if(reponse.isEmpty() || CommonDefine.RESPONSE_OK.equals(reponse)){
//						//删除数据
//						commonManagerMapper.delTableById(T_IMPORT_LOGISTICS, "LOGISTICS_ID",
//								params.get("LOGISTICS_ID"));
//					}else{
//						//抛出错误信息
//						throw new CommonException(new Exception(),
//								MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, reponse);
//					}
//				}
//			}else if(Integer.valueOf(CommonDefine.APP_STATUS_STORE).equals(
//					params.get("APP_STATUS"))){
//				//删除数据
//				commonManagerMapper.delTableById(T_IMPORT_PAY, "PAY_ID",
//						params.get("PAY_ID"));
//			}
			
			
		} 
//		catch (CommonException e) {
//			throw e;
//		} 
		catch (Exception e) {
			throw new CommonException(e,
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR);
		}
	}



	// 生成xml文件
	private String submitXml_SKU(String guid, Map<String, Object> data,int messageType,String currentTime) throws CommonException{
		// 提交需要生成xml文件
			System.out.println("submitXml_SKU_IMPORT_"+messageType);
			
			Map head = importCommonManagerMapper.selectDataForMessage20X_import_head(guid);
			
			head.put("SEND_TIME", currentTime);
			
			switch(messageType){
			case CommonDefine.CEB201:
				head.put("MESSAGE_TYPE", CommonDefine.CEB201);
				break;
			case CommonDefine.CEB202:
				head.put("MESSAGE_TYPE", CommonDefine.CEB202);
				break;
			case CommonDefine.CEB203:
				head.put("MESSAGE_TYPE", CommonDefine.CEB203);
				break;	
			}
			//xml报文
			String resultXmlString = XmlUtil.generalRequestXml4NJ(head, data, null, messageType);
			//获取返回xml字符串
			String response = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_DECLARE);
			
			//获取返回信息
			return response;
	}
	
	
	// 生成xml文件
	private String submitXml_PAY(String guid, Map<String, Object> data,int messageType,String currentTime) throws CommonException{
		// 提交需要生成xml文件
			System.out.println("submitXml_PAY_IMPORT_"+messageType);
			
			Map head = importCommonManagerMapper.selectDataForMessage40X_import_head(guid);
			
			head.put("SEND_TIME", currentTime);
			
			switch(messageType){
			case CommonDefine.CEB401:
				head.put("MESSAGE_TYPE", CommonDefine.CEB401);
				break;
//			case CommonDefine.CEB402:
//				head.put("MESSAGE_TYPE", CommonDefine.CEB402);
//				break;
//			case CommonDefine.CEB403:
//				head.put("MESSAGE_TYPE", CommonDefine.CEB403);
//				break;	
			}
			//xml报文
			String resultXmlString = XmlUtil.generalRequestXml4NJ(head, data, null, messageType);
			//获取返回xml字符串
			String response = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_DECLARE);
			
			//获取返回信息
			return response;
	}

	@Override
	public void batchSubmit_LOGISTICS(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");
		List<Map<String,Object>> logisticsList = null;
		Map logistics = null;
		String tableName=T_IMPORT_LOGISTICS;
		String primaryCol="LOGISTICS_ID";
		String uniqueCol="";
		StringBuilder errorMessage = new StringBuilder("");
		for(String guid:guidList){
			try{
				//获取运单信息
				logisticsList = commonManagerMapper.selectTableListByCol(tableName, "GUID", guid, null, null);
				if(logisticsList !=null && logisticsList.size() == 1){
					logistics = logisticsList.get(0);
				}else{
					errorMessage.append(guid+"：运单数据不存在！;<br/>");
					continue;
				}
				//数据完备性检查
				if(logistics.get(primaryCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+primaryCol+"为空！）;<br/>");
					continue;
				}
				// 唯一性校验
				uniqueCol="LOGISTICS_NO";
				//数据完备性检查
				if(logistics.get(uniqueCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+uniqueCol+"为空！）;<br/>");
					continue;
				}
				uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),primaryCol,logistics.get(primaryCol),false);
				
				uniqueCol="ORDER_NO";
				//数据完备性检查
				if(logistics.get(uniqueCol) == null){
					errorMessage.append(guid+"：提交错误（字段"+uniqueCol+"为空！）;<br/>");
					continue;
				}
				uniqueCheck(tableName,uniqueCol,logistics.get(uniqueCol),primaryCol,logistics.get(primaryCol),false);
				
				Object primaryId=logistics.get(primaryCol);
				
				logistics.put("APP_STATUS", CommonDefine.APP_STATUS_UPLOAD);
				// 生成xml报文
				submitXml_LOGISTICS(logistics,
						Integer.valueOf(primaryId.toString()),
						CommonDefine.CEB511, CommonDefine.LOGISTICS_TYPE_IMPORT);
			}catch(CommonException e){
				errorMessage.append(guid+"：提交错误（"+e.getErrorMessage()+"）;<br/>");
			}
		
		}
		//判断是否有错误信息，如果有抛出异常
		if(!errorMessage.toString().isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, errorMessage.toString());
		}
	}
	
	
	@Override
	public void batchSubmit_LOGISTICS_STATUS(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");
		String newLogisticsStatus = (String) params.get("LOGISTICS_STATUS");
		List<Map<String,Object>> logisticsList = null;
		Map logistics = null;
		String tableName=T_IMPORT_LOGISTICS;
		String primaryCol="LOGISTICS_ID";
		String uniqueCol="";
		StringBuilder errorMessage = new StringBuilder("");

		for (String guid : guidList) {
			// 获取运单信息
			logisticsList = commonManagerMapper.selectTableListByCol(tableName,
					"GUID", guid, null, null);
			if (logisticsList != null && logisticsList.size() == 1) {
				logistics = logisticsList.get(0);
			} else {
				errorMessage.append(guid + "：运单数据不存在！;<br/>");
				continue;
			}

			List<String> keys = Arrays.asList(new String[] {
					"LOGISTICS_STATUS", "RETURN_STATUS", "RETURN_TIME",
					"RETURN_INFO" });
			List<Object> values = Arrays.asList(new Object[] {
					newLogisticsStatus, null, null, null });
			commonManagerMapper.updateTableByNVList(tableName, primaryCol,
					logistics.get(primaryCol), keys, values);
			Object primaryId = logistics.get(primaryCol);

			// 生成xml报文
			// 修改APP_STATUS为upload
			logistics.put("APP_STATUS", CommonDefine.APP_STATUS_UPLOAD);
			submitXml_LOGISTICS(logistics,
					Integer.valueOf(primaryId.toString()), CommonDefine.CEB513,
					CommonDefine.LOGISTICS_TYPE_IMPORT);

		}
		//判断是否有错误信息，如果有抛出异常
		if(!errorMessage.toString().isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, errorMessage.toString());
		}
	}
	
	
	

	@Override
	public void applyExpressNo_LOGISTICS(Map<String, Object> params) throws CommonException {
		List<String> guidList = (List<String>) params.get("guidList");

		// 提交需要生成xml文件
		System.out.println("applyExpressNo_LOGISTICS"+CommonDefine.APPLY_EMS_NO);
		
		Map<String,String> leafNods = new HashMap<String,String>();
		
		leafNods.put("sysAccount", CommonUtil.getSystemConfigProperty("sysAccount"));
		leafNods.put("passWord", CommonUtil.encryptionMD5((CommonUtil.getSystemConfigProperty("passWord"))));
		leafNods.put("appKey", CommonUtil.getSystemConfigProperty("appkey_ems"));
		leafNods.put("businessType", CommonUtil.getSystemConfigProperty("businessType"));
		leafNods.put("billNoAmount", String.valueOf(guidList.size()));
		
		
		//xml报文
		String resultXmlString = XmlUtil.generalCommonXml("XMLInfo", leafNods);
		//获取返回数据
		String soapResponseData = sendHttpCMD(resultXmlString,CommonDefine.APPLY_EMS_NO);

/*			//摘取出返回信息
		String soapResponseData = 
	        "<response>"+
	            "<result>1</result>"+
	            "<errorDesc>3123</errorDesc>"+
				 "<errorCode>11231</errorCode>"+
				  "<assignIds><assignId><billno>22222222</billno></assignId></assignIds>"+
	        "</response>";*/
		if(soapResponseData == null ||soapResponseData.isEmpty()){
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, "无回执信息！");
		}
		Map<String,Object> response = XmlUtil.parseXmlStringForReceipt_EMS(soapResponseData);
		if(response!=null&&response.size()>0){
			//更新数据库，或上报异常
			if (response.get("result") != null
					&& CommonDefine.FAILED == Integer.valueOf(response.get(
							"result").toString())) {
				// 抛出错误信息
				throw new CommonException(new Exception(),
						MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, "【"
								+ response.get("errorCode").toString() + "】"
								+ response.get("errorDesc").toString()+"！");
			}else {
				//更新数据
				List<String> assignIds = (List<String>) response.get("assignIds");
				
				if(assignIds.size()!=guidList.size()){
					//抛出错误信息
					throw new CommonException(new Exception(),
							MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, "返回订单号数量与所选项目数量不一致！");
				}
				//更新数据
				for(int i = 0;i<assignIds.size();i++){
					Map logistics = new HashMap();
					logistics.put("GUID", guidList.get(i));
					logistics.put("PARCEL_INFO", assignIds.get(i));
					commonManagerMapper.updateLogistics(logistics, T_IMPORT_LOGISTICS);
				}
			}
		}else{
			//抛出错误信息
			throw new CommonException(new Exception(),
					MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, soapResponseData);
		}
	}
	

	// 生成xml文件
	public String submitXml_ORDER(String guid, Map<String, Object> data, List<Map> subDataList, int messageType,String currentTime)  throws CommonException{
		
		// 提交需要生成xml文件
		System.out.println("submitXml_ORDER_IMPORT_"+messageType);
		
		Map head = importCommonManagerMapper.selectDataForMessage30X_import_head(guid);
		
		head.put("SEND_TIME", currentTime);
		data.put("APP_TIME", currentTime);
		
		switch(messageType){
		case CommonDefine.CEB301:
			head.put("MESSAGE_TYPE", CommonDefine.CEB301);
			break;
		case CommonDefine.CEB302:
			head.put("MESSAGE_TYPE", CommonDefine.CEB302);
			break;
		case CommonDefine.CEB303:
			head.put("MESSAGE_TYPE", CommonDefine.CEB303);
			break;	
		}
		//xml报文
		String resultXmlString = XmlUtil.generalRequestXml4NJ(head, data, subDataList, messageType);
		//获取返回xml字符串
		String response = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_DECLARE);
		
		//获取返回信息
		return response;
	}
	
	// 生成xml文件
	public String submitXml_INVENTORY(String guid, Map<String, Object> data, List<Map> subDataList, int messageType,String currentTime)  throws CommonException{
		
		// 提交需要生成xml文件
		System.out.println("submitXml_INVENTORY_IMPORT_"+messageType);
		
		Map head = importCommonManagerMapper.selectDataForMessage60X_import_head(guid);
		
		head.put("SEND_TIME", currentTime);
		data.put("APP_TIME", currentTime);
		
		//测试用
//		data.put("BIZ_TYPE", "2");
		
		//关联order中的orderType
		String messageId = head.get("MESSAGE_ID").toString();
		switch(messageType){
		case CommonDefine.CEB601:
			//一般进口
			if("1".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB601);
			}
			//一般出口
			if("2".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB607);
				messageId = messageId.replaceFirst("CEB_601", "CEB_"+CommonDefine.CEB607);
			}
			//保税进口
			if("3".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB604);
				messageId = messageId.replaceFirst("CEB_601", "CEB_"+CommonDefine.CEB604);
			}
			//保税出口
			if("4".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB610);
				messageId = messageId.replaceFirst("CEB_601", "CEB_"+CommonDefine.CEB610);
			}
			head.put("MESSAGE_ID", messageId);
			break;
		case CommonDefine.CEB603:
			//一般进口
			if("1".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB603);
			}
			//一般出口
			if("2".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB609);
				messageId = messageId.replaceFirst("CEB_603", "CEB_"+CommonDefine.CEB609);
			}
			//保税进口
			if("3".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB606);
				messageId = messageId.replaceFirst("CEB_603", "CEB_"+CommonDefine.CEB606);
			}
			//保税出口
			if("4".equals(data.get("BIZ_TYPE").toString())){
				head.put("MESSAGE_TYPE", CommonDefine.CEB612);
				messageId = messageId.replaceFirst("CEB_603", "CEB_"+CommonDefine.CEB612);
			}
			head.put("MESSAGE_ID", messageId);
			break;
		}
		//在进口时，收发货人代码自动填收货人的身份证号，收发货人名称自动填收货人姓名
		//出口时，收发货人代码自动填物流企业代码，收发货人名称自动填物流企业名称
		if("I".equals(data.get("IE_FLAG").toString())){
			data.put("OWNER_CODE", data.get("CONTACT_CODE"));
			data.put("OWNER_NAME", data.get("CONTACT_NAME"));
		}else if("E".equals(data.get("IE_FLAG").toString())){
			data.put("OWNER_CODE", data.get("CODE_CODE"));
			data.put("OWNER_NAME", data.get("CODE_NAME"));
		}
		data.remove("CONTACT_CODE");
		data.remove("CONTACT_NAME");
		data.remove("CODE_CODE");
		data.remove("CODE_NAME");

		//xml报文
		String resultXmlString = XmlUtil.generalRequestXml4NJ(head, data, subDataList, messageType);
		//获取返回xml字符串
		String response = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_DECLARE);
		
		//获取返回信息
		return response;
	}
	
	// 生成xml文件
	private void getReceipt_SKU(String guid, String ebcCode,String itemNo, int messageType) throws CommonException{
		// 提交需要生成xml文件
			System.out.println("getReceipt_SKU_IMPORT_"+messageType);
			
			Map<String,String> leafNods = new HashMap<String,String>();

			switch(messageType){
			case CommonDefine.CEB201_RECEIPT_SINGLE:
				leafNods.put("strEbcCode", ebcCode);
				leafNods.put("strItemNo", itemNo);
				break;
			case CommonDefine.CEB201_RECEIPT_LIST:
				leafNods.put("strEbcCode", ebcCode);
				break;
			}
			//xml报文
			String resultXmlString = XmlUtil.generalReceiptXml4NJ(messageType, leafNods);
			//获取返回数据
			String soapResponseData = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_RECEIPT);
/*			//摘取出返回信息
			soapResponseData = "<NewDataSet>"+
		        "<NJKJ_MESSAGE_APPR_RTN>"+
		            "<EBC_CODE>3215916102</EBC_CODE>"+
		            "<ITEM_NO>G23521506000000261</ITEM_NO>"+
					"<G_NO />"+
		            "<CHK_STATUS>3</CHK_STATUS>"+
		            "<CHK_RESULT>审批意见</CHK_RESULT>"+
					"<CHK_TIME>20150723085544 </CHK_TIME>"+
		        "</NJKJ_MESSAGE_APPR_RTN>"+
		        "<NJKJ_MESSAGE_APPR_RTN>"+
		            "<EBC_CODE>3215916102</EBC_CODE>"+
		           "<ITEM_NO>G23521506000000262</ITEM_NO>"+
		            "<G_NO>32159161020000000012</G_NO>"+
		            "<CHK_STATUS>2</CHK_STATUS>"+
					"<CHK_RESULT />"+
					"<CHK_TIME>20150723085544 </CHK_TIME>"+
		        "</NJKJ_MESSAGE_APPR_RTN>"+
		    "</NewDataSet>";*/
			if(soapResponseData == null ||soapResponseData.isEmpty()){
				//抛出错误信息
				throw new CommonException(new Exception(),
						MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, "无回执信息！");
			}
				List<Map<String,String>> response = XmlUtil.parseXmlStringForReceipt(soapResponseData);
			if(response!=null&&response.size()>0){
				//更新商品数据
				//{EBC_CODE=3215916102, G_NO=, CHK_TIME=20150723085544 , ITEM_NO=G23521506000000261, CHK_STATUS=3, CHK_RESULT=审批意见}
				for(Map data:response){
					if(data.get("EBC_CODE")!=null){
						Map sku = new HashMap();
						sku.put("RETURN_STATUS", data.get("CHK_STATUS"));
						sku.put("RETURN_TIME", data.get("CHK_TIME"));
						sku.put("RETURN_INFO", data.get("CHK_RESULT"));
						sku.put("G_NO", data.get("G_NO"));
						sku.put("GUID", guid);
						//回执状态为3审批通过 更新申报状态 为申报完成
						if (sku.get("RETURN_STATUS") != null
								&& Integer.valueOf(sku.get(
										"RETURN_STATUS").toString()) == CommonDefine.RETURN_STATUS_2) {
							sku.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
						}
						importCommonManagerMapper.updateSku_import(sku);
					}
				}
			}else{
				//抛出错误信息
				throw new CommonException(new Exception(),
						MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, soapResponseData);
			}
	}

	// 生成xml文件
	private void getReceipt_INVENTORY(String guid, String ebcCode,String logisticsNo, int messageType) throws CommonException{
		// 提交需要生成xml文件
			System.out.println("getReceipt_INVENTORY_IMPORT_"+messageType);
			
			Map<String,String> leafNods = new HashMap<String,String>();

			switch(messageType){
			case CommonDefine.CEB601_RECEIPT_SINGLE:
			case CommonDefine.CEB607_RECEIPT_SINGLE:
				leafNods.put("strEbcCode", ebcCode);
				leafNods.put("strLogisticsNo", logisticsNo);
				break;
			case CommonDefine.CEB601_RECEIPT_LIST:
			case CommonDefine.CEB607_RECEIPT_LIST:
				leafNods.put("strEbcCode", ebcCode);
				break;
			}
			//xml报文
			String resultXmlString = XmlUtil.generalReceiptXml4NJ(messageType, leafNods);
			//获取返回数据
			String soapResponseData = sendHttpCMD(resultXmlString,messageType,CommonDefine.CMD_TYPE_RECEIPT);
/*			//摘取出返回信息
			soapResponseData = "<NewDataSet>"+
		        "<NJKJ_MESSAGE_APPR_RTN>"+
		            "<EBC_CODE>3215916102</EBC_CODE>"+
		            "<LOGISTICS_NO>G23521506000000261</LOGISTICS_NO>"+
					 "<PRE_NO>P23001150000000006</PRE_NO>"+
					  "<INVT_NO>D23001150000000006</INVT_NO>"+
		            "<CHK_STATUS>3</CHK_STATUS>"+
		            "<CHK_RESULT>审批意见</CHK_RESULT>"+
					"<CHK_TIME>20150723085544 </CHK_TIME>"+
		        "</NJKJ_MESSAGE_APPR_RTN>"+
		        "<NJKJ_MESSAGE_APPR_RTN>"+
		            "<EBC_CODE>3215916102</EBC_CODE>"+
		          "<LOGISTICS_NO>G23521506000000261</LOGISTICS_NO>"+
					 "<PRE_NO>P23001150000000006</PRE_NO>"+
					  "<INVT_NO>D23001150000000006</INVT_NO>"+
		            "<CHK_STATUS>2</CHK_STATUS>"+
					"<CHK_RESULT />"+
					"<CHK_TIME>20150723085544 </CHK_TIME>"+
		        "</NJKJ_MESSAGE_APPR_RTN>"+
		    "</NewDataSet>";*/
			if(soapResponseData == null ||soapResponseData.isEmpty()){
				//抛出错误信息
				throw new CommonException(new Exception(),
						MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, "无回执信息！");
			}
			List<Map<String,String>> response = XmlUtil.parseXmlStringForReceipt(soapResponseData);
			if(response!=null&&response.size()>0){
				//更新商品数据
				//{EBC_CODE=3215916102, G_NO=, CHK_TIME=20150723085544 , ITEM_NO=G23521506000000261, CHK_STATUS=3, CHK_RESULT=审批意见}
				for(Map data:response){
					if(data.get("EBC_CODE")!=null){
						Map inventory = new HashMap();
						inventory.put("RETURN_STATUS", data.get("CHK_STATUS"));
						inventory.put("RETURN_TIME", data.get("CHK_TIME"));
						inventory.put("RETURN_INFO", data.get("CHK_RESULT"));
						inventory.put("INVT_NO", data.get("INVT_NO"));
						inventory.put("GUID", guid);
						//回执状态为3审批通过 更新申报状态 为申报完成
						if (inventory.get("RETURN_STATUS") != null
								&& Integer.valueOf(inventory.get(
										"RETURN_STATUS").toString()) == CommonDefine.RETURN_STATUS_2) {
							inventory.put("APP_STATUS",CommonDefine.APP_STATUS_COMPLETE);
						}
						importCommonManagerMapper.updateInventory_import(inventory);
					}
				}
			}else{
				//抛出错误信息
				throw new CommonException(new Exception(),
						MessageCodeDefine.COM_EXCPT_INTERNAL_ERROR, soapResponseData);
			}
	}

	//比较旧数据与新数据，筛选出修改过的字段信息
	private Map compareData(Map newData,Map oldData){
		Map result = new HashMap();
		for(Object key:newData.keySet()){
			if(oldData.containsKey(key) && !newData.get(key).equals(oldData.get(key))){
				if(BigDecimal.class.isInstance(oldData.get(key)) || Double.class.isInstance(oldData.get(key))){
					if(Double.valueOf(oldData.get(key).toString()).compareTo(Double.valueOf(newData.get(key).toString())) != 0){
						result.put(key, newData.get(key));
					}
				}else{
					result.put(key, newData.get(key));
				}
			}
		}
		return result;
	}


}
