import com.google.gson.Gson
import com.webasyst.api.webasyst.InstallationInfo
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InstallationInfoTest {
    @Test
    fun testGradientIcon() {
        val gson = Gson()
        val str = """{"name":"КП","logo":{"mode":"gradient","text":{"value":"КП","color":"#FFFFFF","default_value":"","default_color":"#fff","formatted_value":"КП"},"two_lines":true,"gradient":{"from":"#383838","to":"#0F0F0F","angle":"0"},"image":{"thumbs":[],"original":[]}}}"""
        val info = gson.fromJson(str, InstallationInfo::class.java)
        assertNotNull(info.logo)
        assertNotNull(info.logo!!.image)
        assertNull(info.logo!!.image!!.original)
    }
}
