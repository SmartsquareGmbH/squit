import org.dom4j.Element

actualResponse.selectNodes("test").each {
    (it as Element).name = "nice"
}
