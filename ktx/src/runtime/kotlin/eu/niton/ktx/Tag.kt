package eu.niton.ktx

interface Tag {
    fun setAttribute(key: String, value: ()->String?)
    fun setEventHandler(key: String, handler: ()->Unit)
    fun setAttributes(attributes: Map<String, (()->String?)?>){
        attributes.forEach { (key, value) -> if(value != null) setAttribute(key, value) }
    }
    fun setEventHandlers(attributes: Map<String, (()->Unit)?>) {
        attributes.forEach { (key, value) -> if(value != null) setEventHandler(key, value) }
    }
    fun setContentFunction(content: ()->Unit)
    fun insertText(stringContent: () -> String)
}