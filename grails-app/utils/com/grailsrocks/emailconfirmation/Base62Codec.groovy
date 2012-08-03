package com.grailsrocks.emailconfirmation

class Base62Codec {
    static BASE62 = (('a'..'z')+('A'..'Z')+('0'..'9')).join()
    
    static encode = { value ->
        if (!(value instanceof byte[]) || (value instanceof Byte[])) {
            value = value?.toString().getBytes('UTF-8')
        }
        def base10 = new BigInteger(1, value)

        def digitMax = BASE62.size().toInteger()

        def tiny = new StringBuilder()

        while (base10 != 0) {
            def digit = base10.mod(digitMax)
            base10 = base10.divide(digitMax)
            tiny << BASE62[digit]
        }

        tiny = tiny.reverse()
    }
    
    static decode = { value ->
        BigInteger v = 0
        def digitMax = BASE62.size().toInteger()

        value.toString().each { c ->
            v *= digitMax
            v += BASE62.indexOf(c)
        }
        
        return v.toByteArray()    
    }
    
    
}