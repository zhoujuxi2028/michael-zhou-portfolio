// 临时调试脚本 - 检查CreateProductRequest的可用方法
import com.ctg.ag.sdk.biz.aep_product_management.CreateProductRequest;
import java.lang.reflect.Method;

public class debug_methods {
    public static void main(String[] args) {
        CreateProductRequest request = new CreateProductRequest();

        System.out.println("CreateProductRequest 可用方法:");
        Method[] methods = request.getClass().getMethods();

        for (Method method : methods) {
            String methodName = method.getName();
            // 只显示set开头的方法
            if (methodName.startsWith("set")) {
                System.out.println("  " + methodName + "(" +
                    java.util.Arrays.toString(method.getParameterTypes()) + ")");
            }
        }
    }
}