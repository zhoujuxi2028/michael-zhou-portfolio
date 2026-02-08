import com.ctg.ag.sdk.biz.aep_product_management.UpdateProductRequest;
import com.ctg.ag.sdk.biz.aep_product_management.DeleteProductRequest;
import java.lang.reflect.Method;

public class debug_update_methods {
    public static void main(String[] args) {
        System.out.println("UpdateProductRequest 方法:");
        Method[] updateMethods = UpdateProductRequest.class.getMethods();
        for (Method method : updateMethods) {
            if (method.getName().startsWith("set")) {
                System.out.println("  " + method.getName() + "(" +
                    java.util.Arrays.toString(method.getParameterTypes()) + ")");
            }
        }

        System.out.println("\nDeleteProductRequest 方法:");
        Method[] deleteMethods = DeleteProductRequest.class.getMethods();
        for (Method method : deleteMethods) {
            if (method.getName().startsWith("set")) {
                System.out.println("  " + method.getName() + "(" +
                    java.util.Arrays.toString(method.getParameterTypes()) + ")");
            }
        }
    }
}
