import com.webasyst.api.shop.OrderList
import com.webasyst.api.util.GsonInstance
import org.junit.Test
import kotlin.test.assertNotNull

class OrderListTest {
    @Test
    fun testOrderList() {
        val gson by GsonInstance
        val orders = javaClass.classLoader!!.getResourceAsStream("order-list-1.json").use {
            it.reader().use { reader ->
                gson.fromJson(reader, OrderList::class.java)
            }
        }
        // Just test that orders deserialize without exceptions
        assertNotNull(orders)
    }
}
