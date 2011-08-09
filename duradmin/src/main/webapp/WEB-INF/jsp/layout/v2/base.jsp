<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="UTF-8" %>
<%@ page session="false"%>
<%@include file="/WEB-INF/jsp/include.jsp" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<!-- 
	created by Daniel Bernstein and CH
 -->
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="language" content="en" />
    <title><spring:message code="application.title" /> :: <tiles:insertAttribute name="title"/></title>
	<link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" />
	<link rel="stylesheet"  href="${pageContext.request.contextPath}/style/jquery-ui.css" type="text/css" />	
	<link rel="stylesheet"  href="${pageContext.request.contextPath}/style/base.css" type="text/css" />	
	<link rel="stylesheet"  href="${pageContext.request.contextPath}/style/flex.css" type="text/css" />
	<link rel="stylesheet"  href="${pageContext.request.contextPath}/style/dialogs.css" type="text/css" />
	<link rel="stylesheet"  href="${pageContext.request.contextPath}/style/buttons.css" type="text/css" />

	<!-- jquery core, ui and css -->
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/jquery.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/jquery-ui.js"></script>
	<!-- 3rd party jquery plugins start-->
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/jquery/plugins/jquery.layout.js"></script>
	<!-- 
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/jquery/plugins/jquery.layout/jquery.layout-1.3.0.rc29.15.js"></script>
	 -->
	
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/jquery/plugins/jquery.ba-throttle-debounce.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/jquery/plugins/jquery.form.js"></script>
	<script type="text/javascript"
		src="${pageContext.request.contextPath}/jquery/plugins/jquery-validate/jquery.validate.js"></script>

	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/ext/jquery.fn.ext.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/ext/jquery.dc.common.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/widget/ui.onoffswitch.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/widget/ui.selectablelist.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/widget/ui.listdetailviewer.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/widget/ui.expandopanel.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/api/durastore-api.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/jquery/dc/api/duraservice-api.js"></script>

	<script type="text/javascript">

	$(function() {

		///////////////////////////////////////////////////////////////////////
		////controls rollovers on tags and properties
		///////////////////////////////////////////////////////////////////////
		$(".dc-mouse-panel-activator td, li.dc-mouse-panel-activator, .dc-mouse-panel").live("mouseover",function(evt){
			var ancestor = $(evt.target).nearestOfClass(".dc-mouse-panel-activator");
			$(".dc-mouse-panel",ancestor).css("visibility","visible");
		}).live("mouseout",function(evt){
			var ancestor = $(evt.target).nearestOfClass(".dc-mouse-panel-activator");
			$(".dc-mouse-panel",ancestor).css("visibility","hidden");
		});
		
		$(".dc-mouse-panel").css("visibility", "hidden");
		
		///////////////////////////////////////////////////////////////////////
		////Layout Page Frame
		///////////////////////////////////////////////////////////////////////
		var pageHeaderLayout;
		 $("body").layout({
				north__size:	    87
			,   north__paneSelector:"#page-header"
			,   resizable:   false
			,   slidable:    false
			,   spacing_open:			0			
			,	togglerLength_open:		0			
			,	togglerLength_closed:	-1
			,	useStateCookie:		true
			,   center__paneSelector: "#page-content"
			,	center__onresize:	"centerLayout.resizeAll"
		});
	});	
	</script>
	<!-- page level header extensions reserved for pages that wish to inject page specific scripts into the header -->
	<tiles:insertAttribute name="header-extensions" ignore="true"/>
			
</head>
<body>
	<tiles:insertAttribute name="body"/>
</body>
</html>
