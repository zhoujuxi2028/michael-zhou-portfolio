import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.junit.Test;

import com.ctg.ag.sdk.core.constant.Scheme;
import com.ctg.ag.sdk.core.model.ApiCallBack;
import org.apache.http.conn.HttpClientConnectionManager;

import com.ctg.ag.sdk.biz.AepDeviceManagementClient;
import com.ctg.ag.sdk.biz.aep_device_management.*;


public class AepDeviceManagementDemo {

	@Test
	public void testApi() throws Exception {

	    // 从环境变量读取认证信息
	    String appKey = System.getenv("AEP_APP_KEY") != null ?
	        System.getenv("AEP_APP_KEY") : "YOUR_APP_KEY";
	    String appSecret = System.getenv("AEP_APP_SECRET") != null ?
	        System.getenv("AEP_APP_SECRET") : "YOUR_APP_SECRET";

	    AepDeviceManagementClient client = AepDeviceManagementClient.newClient()
  .appKey(appKey).appSecret(appSecret)
  .build();

		QueryDeviceListRequest request = new QueryDeviceListRequest();
		// set your request params here
		// 从环境变量读取MasterKey
		String masterKey = System.getenv("AEP_MASTER_KEY") != null ?
		    System.getenv("AEP_MASTER_KEY") : "YOUR_MASTER_KEY";
		request.setParamMasterKey(masterKey);	// single value
		// 从环境变量读取产品ID
		String productId = System.getenv("AEP_PRODUCT_ID");
		if (productId != null) {
		    request.setParamProductId(Integer.parseInt(productId));
		} else {
		    request.setParamProductId(null);  // 查询所有产品的设备
		}
		request.setParamSearchValue(null);	// single value
		request.setParamPageNow(null);	// single value
		request.setParamPageSize(null);	// single value
		System.out.println(client.QueryDeviceList(request));

		// more requests

		client.shutdown();

	}

}