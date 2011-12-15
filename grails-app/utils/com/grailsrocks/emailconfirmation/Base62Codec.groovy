package com.grailsrocks.emailconfirmation

class Base62Codec {
    static BASE62 = (('a'..'z')+('A'..'Z')+('0'..'9')).join()
/*

def r = new Random()
def inputBytes = []
32.times { inputBytes << r.nextInt(256) - 128 }
println "inBytes "+inputBytes

def base10 = new BigInteger(inputBytes as byte[])

def base62 = (('a'..'z')+('A'..'Z')+('0'..'9')).join()
def digitMax = base62.size().toInteger()
println base62

def tinyUrl = new StringBuilder()

println "base10 is a ${base10.class} - $base10"

while (base10 != 0) {
    def digit = base10.mod(digitMax)
    base10 = base10.divide(digitMax)
    tinyUrl << base62[digit]
}

tinyUrl = tinyUrl.reverse()

println tinyUrl

println "reversing it ........."


BigInteger value = 0

tinyUrl.toString().each { c ->
    value *= digitMax
    value += base62.indexOf(c)
}

println "Decoded to: ${value}"

def decoded = new String(value.toByteArray())
println "Decoded string: $decoded"
*/
    
    static encode = { value ->
        if (!(value instanceof byte[]) || (value instanceof Byte[])) {
            value = value?.toString().getBytes('UTF-8')
        }
        def base10 = new BigInteger(value)

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