import org.dom4j.Element

request.selectNodes("//animals").each {
    (it as Element).addAttribute("test", "test")
}
