<%@ page language="java" contentType="text/html; charset=gb2312"
    pageEncoding="gb2312"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=gb2312">
<title>功能管理</title>
<link href="css/tq.css" rel="stylesheet" type="text/css">
<link href="css/iconbuttons.css" rel="stylesheet" type="text/css">

<script src="js/tq.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.public.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.datatable.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.form.js?0817" type="text/javascript">//表单</script>
<script src="js/tq.searchform.js?0817" type="text/javascript">//查询表单</script>
<script src="js/tq.window.js?0817" type="text/javascript">//弹窗</script>
<script src="js/tq.hash.js?0817" type="text/javascript">//哈希</script>
<script src="js/tq.stab.js?0817" type="text/javascript">//切换</script>
<script src="js/tq.validata.js?0817" type="text/javascript">//验证</script>
<script src="js/My97DatePicker/WdatePicker.js" type="text/javascript">//日期</script>
</head>
<body>
<div id="functionobj" style="width:100%;height:100%;margin:0px;"></div>
<script language="javascript">
/*权限*/
var orglist = eval(T.A.sendData("getdata.do?action=getorg"));
var funclist = eval(T.A.sendData("getdata.do?action=getfunc"));
var _mediaField = [
		{fieldcnname:"编号",fieldname:"id",fieldvalue:'',inputtype:"text",twidth:"100" ,height:"",edit:false,issort:false,fhide:true},
		{fieldcnname:"名称",fieldname:"nname",fieldvalue:'',inputtype:"text",twidth:"100" ,height:"",issort:false},
		{fieldcnname:"组织类型",fieldname:"oid",fieldvalue:'',inputtype:"select",noList:orglist,twidth:"100" ,height:"",issort:false,edit:false,hide:true},
		/* {fieldcnname:"状态",fieldname:"state",fieldvalue:'',inputtype:"select",noList:[{"value_no":-1,"value_name":"全部"},{"value_no":0,"value_name":"正常"},{"value_no":1,"value_name":"禁用"}],twidth:"100" ,height:"",issort:false,edit:false,hide:true}, */
		{fieldcnname:"父功能",fieldname:"pid",fieldvalue:'',inputtype:"select",noList:funclist,twidth:"100" ,height:"",issort:false,edit:false,hide:true},
		{fieldcnname:"请求地址",fieldname:"url",fieldvalue:'',inputtype:"text",twidth:"200" ,height:"",issort:false},
		{fieldcnname:"子权限",fieldname:"sub_auth",fieldvalue:'',inputtype:"text",twidth:"500" ,height:"",issort:false},
		{fieldcnname:"Action(子权限)",fieldname:"actions",fieldvalue:'',inputtype:"text",twidth:"500" ,height:"",issort:false}
	];

var _functionT = new TQTable({
	tabletitle:"功能管理",
	ischeck:false,
	tablename:"function_tables",
	dataUrl:"function.do",
	iscookcol:false,
	//dbuttons:false,
	buttons:getAuthButtons(),
	//searchitem:true,
	param:"action=query&pid=${pid}&oid=${oid}",
	tableObj:T("#functionobj"),
	fit:[true,true,true],
	tableitems:_mediaField,
	isoperate:getAuthIsoperateButtons()
});
function getAuthButtons(){
	var bts=[];
	bts.push({dname:"添加功能",icon:"edit_add.png",onpress:function(Obj){
				Twin({Id:"function_add",Title:"添加功能",Width:550,sysfun:function(tObj){
					Tform({
						formname: "parking_edit_f",
						formObj:tObj,
						recordid:"id",
						suburl:"function.do?action=create&pid=${pid}&oid=${oid}",
						method:"POST",
						Coltype:2,
						formAttr:[{
							formitems:[{kindname:"",kinditemts:_mediaField}]
						}],
						buttons : [//工具
							{name: "cancel", dname: "取消", tit:"取消添加",icon:"cancel.gif", onpress:function(){TwinC("function_add");} }
						],
						Callback:
						function(f,rcd,ret,o){
							if(ret=="1"){
								T.loadTip(1,"添加成功！",2,"");
								TwinC("function_add");
								_functionT.M();
							}else {
								T.loadTip(1,ret,2,o);
							}
						}
					});	
				}
			});
		}});
	if(bts.length>0)
		return bts;
	return false;
}
function getAuthIsoperateButtons(){
	var bts = [];
	bts.push({name:"编辑",fun:function(id){
		T.each(_functionT.tc.tableitems,function(o,j){
			o.fieldvalue = _functionT.GD(id)[j]
		});
		Twin({Id:"function_edit_"+id,Title:"编辑",Width:550,sysfunI:id,sysfun:function(id,tObj){
				Tform({
					formname: "function_edit_f",
					formObj:tObj,
					recordid:"function_id",
					suburl:"function.do?action=edit&id="+id,
					method:"POST",
					Coltype:2,
					formAttr:[{
						formitems:[{kindname:"",kinditemts:_functionT.tc.tableitems}]
					}],
					buttons : [//工具
						{name: "cancel", dname: "取消", tit:"取消编辑",icon:"cancel.gif", onpress:function(){TwinC("function_edit_"+id);} }
					],
					Callback:
					function(f,rcd,ret,o){
						if(ret=="1"){
							T.loadTip(1,"编辑成功！",2,"");
							TwinC("function_edit_"+id);
							_functionT.M();
						}else{
							T.loadTip(1,ret,2,o)
						}
					}
				});	
			}
		})
	}});
	bts.push({name:"删除",fun:function(id){
		Tconfirm({Title:"确认删除吗",Content:"确认删除吗",OKFn:function(){
		T.A.sendData("function.do?action=delete","post","id="+id,
			function deletebackfun(ret){
				if(ret=="1"){
					T.loadTip(1,"删除成功！",2,"");
					_functionT.M()
				}else{
					T.loadTip(1,ret,2,"");
				}
			}
		)}})
	}}
	);
	
	if(bts.length <= 0){return false;}
	return bts;
}


_functionT.C();
</script>

</body>
</html>
