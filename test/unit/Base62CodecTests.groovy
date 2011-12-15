import com.grailsrocks.emailconfirmation.Base62Codec

class Base62CodecTests extends GroovyTestCase {
    void testEncodingBytesIsSymmetrical() {
        def enc = Base62Codec.encode((1..100) as byte[])
        def dec = Base62Codec.decode(enc)
        assertEquals 100, dec.size()
        int i = 1
        assertTrue dec.every { it -> return it == i++ }
    }

    void testEncodingStringsIsSymmetrical() {
        def s = "Hello world"
        def enc = Base62Codec.encode(s)
        def dec = Base62Codec.decode(enc)
        assertEquals s, new String(dec, 'UTF-8')
    }
}