package me.vasler.protection;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.*;
import sun.management.VMManagement;
import sun.misc.Unsafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class AntiDump {

    private static final Unsafe unsafe;
    private static Method findNative;
    private static ClassLoader classLoader;

    private static boolean ENABLE;

    private static final String[] naughtyFlags = {
            "-XBootclasspath",
            "-javaagent",
            "-Xdebug",
            "-agentlib",
            "-Xrunjdwp",
            "-Xnoagent",
            "-verbose",
            "-DproxySet",
            "-DproxyHost",
            "-DproxyPort",
            "-Djavax.net.ssl.trustStore",
            "-Djavax.net.ssl.trustStorePassword"
    };

    /* UnsafeProvider */
    static {
        Unsafe ref;
        try {
            Class<?> clazz = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = clazz.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            ref = (Unsafe) theUnsafe.get(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            ref = null;
        }

        unsafe = ref;
    }

    /* CookieFuckery */
    public static void check() {
        if (!ENABLE) return;
        try {
            virusTotal();
            Field jvmField = ManagementFactory.getRuntimeMXBean().getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);
            VMManagement jvm = (VMManagement) jvmField.get(ManagementFactory.getRuntimeMXBean());
            List<String> inputArguments = jvm.getVmArguments();

            for (String arg : naughtyFlags) {
                for (String inputArgument : inputArguments) {
                    if (inputArgument.contains(arg)) {
                        System.out.println("Found illegal program arguments!");
                        dumpDetected();
                    }
                }
            }
            try {
                byte[] bytes = createDummyClass("java/lang/instrument/Instrumentation");
                unsafe.defineClass("java.lang.instrument.Instrumentation", bytes, 0, bytes.length, null, null);
            } catch (Throwable e) {
                e.printStackTrace();
                dumpDetected();
            }
            if (isClassLoaded("sun.instrument.InstrumentationImpl")) {
                System.out.println("Found sun.instrument.InstrumentationImpl!");
                dumpDetected();
            }

            byte[] bytes = createDummyClass("dummy/class/path/MaliciousClassFilter");
            unsafe.defineClass("dummy.class.path.MaliciousClassFilter", bytes, 0, bytes.length, null, null); // Change this.
            System.setProperty("sun.jvm.hotspot.tools.jcore.filter", "dummy.class.path.MaliciousClassFilter"); // Change this.

            disassembleStruct();

        } catch (Throwable e) {
            e.printStackTrace();
            dumpDetected();
        }
    }

    private static boolean isClassLoaded(@SuppressWarnings("SameParameterValue") String clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        m.setAccessible(true);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        return m.invoke(cl, clazz) != null || m.invoke(scl, clazz) != null;
    }


    /* DummyClassProvider */
    private static byte[] createDummyClass(String name) {
        ClassNode classNode = new ClassNode();
        classNode.name = name.replace('.', '/');
        classNode.access = ACC_PUBLIC;
        classNode.version = V1_8;
        classNode.superName = "java/lang/Object";

        List<MethodNode> methods = new ArrayList<>();
        MethodNode methodNode = new MethodNode(ACC_PUBLIC + ACC_STATIC, "<clinit>", "()V", null, null);

        InsnList insn = new InsnList();
        insn.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        insn.add(new LdcInsnNode("Nice try"));
        insn.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
        insn.add(new TypeInsnNode(NEW, "java/lang/Throwable"));
        insn.add(new InsnNode(DUP));
        insn.add(new LdcInsnNode("owned"));
        insn.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Throwable", "<init>", "(Ljava/lang/String;)V", false));
        insn.add(new InsnNode(ATHROW));

        methodNode.instructions = insn;

        methods.add(methodNode);
        classNode.methods = methods;

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static void dumpDetected() {
        try {
            unsafe.putAddress(0, 0);
        } catch (Exception e) {}
        System.exit(0); // Shutdown.
        Error error = new Error();
        error.setStackTrace(new StackTraceElement[]{});
        throw error;
    }

    /* StructDissasembler */
    private static void resolveClassLoader() throws NoSuchMethodException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String vmName = System.getProperty("java.vm.name");
            String dll = vmName.contains("Client VM") ? "/bin/client/jvm.dll" : "/bin/server/jvm.dll";
            try {
                System.load(System.getProperty("java.home") + dll);
            } catch (UnsatisfiedLinkError e) {
                throw new RuntimeException(e);
            }
            classLoader = AntiDump.class.getClassLoader();
        } else {
            classLoader = null;
        }

        findNative = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);

        try {
            Class<?> cls = ClassLoader.getSystemClassLoader().loadClass("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            unsafe.putObjectVolatile(cls, unsafe.staticFieldOffset(logger), null);
        } catch (Throwable t) {
            // 10
        }

        findNative.setAccessible(true);
    }

    private static void setupIntrospection() throws Throwable {
        resolveClassLoader();
    }

    public static void disassembleStruct() {
        try {
            setupIntrospection();
            long entry = getSymbol("gHotSpotVMStructs");
            unsafe.putLong(entry, 0);
        } catch (Throwable t) {
            t.printStackTrace();
            dumpDetected();
        }
    }

    private static long getSymbol(String symbol) throws InvocationTargetException, IllegalAccessException {
        long address = (Long) findNative.invoke(null, classLoader, symbol);
        if (address == 0)
            throw new NoSuchElementException(symbol);

        return unsafe.getLong(address);
    }

    public static void virusTotal() throws InterruptedException, IOException {
        List<String> blacklistedHWIDs = Arrays.asList(
                "00000000-0000-0000-0000-000000000000",
                "00000000-0000-0000-0000-50E5493391EF",
                "00000000-0000-0000-0000-AC1F6BD047A0",
                "00000000-0000-0000-0000-AC1F6BD04850",
                "00000000-0000-0000-0000-AC1F6BD048D6",
                "00000000-0000-0000-0000-AC1F6BD048DC",
                "00000000-0000-0000-0000-AC1F6BD048F8",
                "00000000-0000-0000-0000-AC1F6BD048FE",
                "00000000-0000-0000-0000-AC1F6BD04900",
                "00000000-0000-0000-0000-AC1F6BD0491C",
                "00000000-0000-0000-0000-AC1F6BD04926",
                "00000000-0000-0000-0000-AC1F6BD04928",
                "00000000-0000-0000-0000-AC1F6BD04972",
                "00000000-0000-0000-0000-AC1F6BD04976",
                "00000000-0000-0000-0000-AC1F6BD04978",
                "00000000-0000-0000-0000-AC1F6BD04986",
                "00000000-0000-0000-0000-AC1F6BD049B8",
                "00000000-0000-0000-0000-AC1F6BD04C0A",
                "00000000-0000-0000-0000-AC1F6BD04D06",
                "00000000-0000-0000-0000-AC1F6BD04D08",
                "00000000-0000-0000-0000-AC1F6BD04D8E",
                "00000000-0000-0000-0000-AC1F6BD04D98",
                "00000000-0000-0000-0000-AC1F6BD04DC0",
                "00000000-0000-0000-0000-AC1F6BD04DCC",
                "02AD9898-FA37-11EB-AC55-1D0C0A67EA8A",
                "032E02B4-0499-05C3-0806-3C0700080009",
                "03AA02FC-0414-0507-BC06-D70700080009",
                "03D40274-0435-05BF-D906-D20700080009",
                "03DE0294-0480-05DE-1A06-350700080009",
                "050C3342-FADD-AEDF-EF24-C6454E1A73C9",
                "05790C00-3B21-11EA-8000-3CECEF4400D0",
                "0700BEF3-1410-4284-81B1-E5C17FA9E18F",
                "07AF2042-392C-229F-8491-455123CC85FB",
                "07E42E42-F43D-3E1C-1C6B-9C7AC120F3B9",
                "08C1E400-3C56-11EA-8000-3CECEF43FEDE",
                "0910CBA3-B396-476B-A7D7-716DB90F5FB9",
                "0934E336-72E4-4E6A-B3E5-383BD8E938C3",
                "0A36B1E3-1F6B-47DE-8D72-D4F46927F13F",
                "0A9D60D4-9A32-4317-B7C0-B11B5C677335",
                "0D748400-3B00-11EA-8000-3CECEF44007E",
                "0F377508-5106-45F4-A0D6-E8352F51A8A5",
                "104F9B96-5B46-4567-BF56-0066C1C6F7F0",
                "11111111-2222-3333-4444-555555555555",
                "119602E8-92F9-BD4B-8979-DA682276D385",
                "12204D56-28C0-AB03-51B7-44A8B7525250",
                "12EE3342-87A2-32DE-A390-4C2DA4D512E9",
                "138D921D-680F-4145-BDFF-EC463E70C77D",
                "13A61742-AF45-EFE4-70F4-05EF50767784",
                "14692042-A78B-9563-D59D-EB7DD2639037",
                "1AAD2042-66E8-C06A-2F81-A6A4A6A99093",
                "1B5D3FFD-A28E-4F11-9CD6-FF148989548C",
                "1D4D3342-D6C4-710C-98A3-9CC6571234D5",
                "213D2878-0E33-4D8C-B0D1-31425B9DE674",
                "222EFE91-EAE3-49F1-8E8D-EBAE067F801A",
                "26645000-3B67-11EA-8000-3CECEF440124",
                "2AB86800-3C50-11EA-8000-3CECEF440130",
                "2C5C2E42-E7B1-4D75-3EA3-A325353CDB72",
                "2CEA2042-9B9B-FAC1-44D8-159FE611FCCC",
                "2DD1B176-C043-49A4-830F-C623FFB88F3C",
                "2E6FB594-9D55-4424-8E74-CE25A25E36B0",
                "2F94221A-9D07-40D9-8C98-87CB5BFC3549",
                "2FBC3342-6152-674F-08E4-227A81CBD5F5",
                "34419E14-4019-11EB-9A22-6C4AB634B69A",
                "361E3342-9FAD-AC1C-F1AD-02E97892270F",
                "365B4000-3B25-11EA-8000-3CECEF44010C",
                "38813342-D7D0-DFC8-C56F-7FC9DFE5C972",
                "38AB3342-66B0-7175-0B23-F390B3728B78",
                "3A9F3342-D1F2-DF37-68AE-C10F60BFB462",
                "3EDC0561-C455-4D64-B176-3CFBBBF3FA47",
                "3F284CA4-8BDF-489B-A273-41B44D668F6D",
                "3F3C58D1-B4F2-4019-B2A2-2A500E96AF2E",
                "3FADD8D6-3754-47C4-9BFF-0E35553DD5FB",
                "40384E87-1FBA-4096-9EA1-D110F0EA92A8",
                "40F100F9-401C-487D-8D37-48107C6CE1D3",
                "418F0D5B-FCB6-41F5-BDA5-94C1AFB240ED",
                "41B73342-8EA1-E6BF-ECB0-4BC8768D86E9",
                "42A82042-3F13-512F-5E3D-6BF4FFFD8518",
                "44B94D56-65AB-DC02-86A0-98143A7423BF",
                "4729AEB0-FC07-11E3-9673-CE39E79C8A00",
                "481E2042-A1AF-D390-CE06-A8F783B1E76A",
                "48941AE9-D52F-11DF-BBDA-503734826431",
                "49434D53-0200-9036-2500-369025000C65",
                "49434D53-0200-9036-2500-369025003865",
                "49434D53-0200-9036-2500-369025003A65",
                "49434D53-0200-9036-2500-369025003AF0",
                "49434D53-0200-9036-2500-369025005CF0",
                "49434D53-0200-9036-2500-36902500F022",
                "49434D53-0200-9065-2500-659025002274",
                "49434D53-0200-9065-2500-659025005073",
                "49434D53-0200-9065-2500-659025008074",
                "49434D53-0200-9065-2500-65902500E439",
                "499B0800-3C18-11EA-8000-3CECEF43FEA4",
                "4C4C4544-0050-3710-8058-CAC04F59344A",
                "4CB82042-BA8F-1748-C941-363C391CA7F3",
                "4CE94980-D7DA-11DD-A621-08606E889D9B",
                "4D4DDC94-E06C-44F4-95FE-33A1ADA5AC27",
                "4DC32042-E601-F329-21C1-03F27564FD6C",
                "4EDF3342-E7A2-5776-4AE5-57531F471D56",
                "51646514-93E1-4CB6-AF29-036B45D14CBF",
                "52A1C000-3BAB-11EA-8000-3CECEF440204",
                "56B9F600-3C1C-11EA-8000-3CECEF4401DE",
                "59C68035-4B21-43E8-A6A6-BD734C0EE699",
                "5BD24D56-789F-8468-7CDC-CAA7222CC121",
                "5C1CA40D-EF14-4DF8-9597-6C0B6355D0D6",
                "5CC7016D-76AB-492D-B178-44C12B1B3C73",
                "5E3E7FE0-2636-4CB7-84F5-8D2650FFEC0E",
                "5E573342-6093-4F2D-5F78-F51B9822B388",
                "5EBC5C00-3B70-11EA-8000-3CECEF4401DA",
                "5EBD2E42-1DB8-78A6-0EC3-031B661D5C57",
                "60C83342-0A97-928D-7316-5F1080A78E72",
                "612F079A-D69B-47EA-B7FF-13839CD17404",
                "63203342-0EB0-AA1A-4DF5-3FB37DBB0670",
                "63DE70B4-1905-48F2-8CC4-F7C13B578B34",
                "63FA3342-31C7-4E8E-8089-DAFF6CE5E967",
                "64176F5E-8F74-412F-B3CF-917EFA5FB9DB",
                "6608003F-ECE4-494E-B07E-1C4615D1D93C",
                "66729280-2B0C-4BD0-8131-950D86871E54",
                "66CC1742-AAC7-E368-C8AE-9EEB22BD9F3B",
                "671BC5F7-4B0F-FF43-B923-8B1645581DC8",
                "67442042-0F69-367D-1B2E-1EE846020090",
                "67C5A563-3218-4718-8251-F38E3F6A89C1",
                "67E595EB-54AC-4FF0-B5E3-3DA7C7B547E3",
                "686D4936-87C1-4EBD-BEB7-B3D92ECA4E28",
                "6881083C-EE5A-43E7-B7E3-A0CE9227839C",
                "69AEA650-3AE3-455C-9F80-51159BAE5EAE",
                "6A669639-4BD2-47E5-BE03-9CBAFC9EF9B3",
                "6AA13342-49AB-DC46-4F28-D7BDDCE6BE32",
                "6ECEAF72-3548-476C-BD8D-73134A9182C8",
                "6F3CA5EC-BEC9-4A4D-8274-11168F640058",
                "71522042-DA0B-6793-668B-CE95AEA7FE21",
                "72492D47-52EF-427A-B623-D4F2192F97D4",
                "73163342-B704-86D5-519B-18E1D191335C",
                "777D84B3-88D1-451C-93E4-D235177420A7",
                "782ED390-AE10-4727-A866-07018A8DED22",
                "79AF5279-16CF-4094-9758-F88A616D81B4",
                "7A484800-3B19-11EA-8000-3CECEF440122",
                "7AB5C494-39F5-4941-9163-47F54D6D5016",
                "7CA33342-A88C-7CD1-1ABB-7C0A82F488BF",
                "7D341C16-E8E9-42EA-8779-93653D877231",
                "7D6A0A6D-394E-4179-9636-662A8D2C7304",
                "7E4755A6-7160-4982-8F5D-6AA481749F10",
                "80152042-2F34-11D1-441F-5FADCA01996D",
                "83BFD600-3C27-11EA-8000-3CECEF4400B4",
                "844703CF-AA4E-49F3-9D5C-74B8D1F5DCB6",
                "84782042-E646-50A0-159F-A8E75D4F9402",
                "84FE3342-6C67-5FC6-5639-9B3CA3D775A1",
                "84FEEFBC-805F-4C0E-AD5B-A0042999134D",
                "8703841B-3C5E-461C-BE72-1747D651CE89",
                "88DC3342-12E6-7D62-B0AE-C80E578E7B07",
                "8B4E8278-525C-7343-B825-280AEBCD3BCB",
                "8DA62042-8B59-B4E3-D232-38B29A10964A",
                "8EC60B88-7F2B-42DA-B8C3-4E2EF2A8C603",
                "907A2A79-7116-4CB6-9FA5-E5A58C4587CD",
                "90A83342-D7E7-7A14-FFB3-2AA345FDBC89",
                "91625303-5211-4AAC-9842-01A41BA60D5A",
                "91A9EEDB-4652-4453-AC5B-8E92E68CBCF5",
                "921E2042-70D3-F9F1-8CBD-B398A21F89C6",
                "94515D88-D62B-498A-BA7C-3614B5D4307C",
                "95BF6A00-3C63-11EA-8000-3CECEF43FEB8",
                "96BB3342-6335-0FA8-BA29-E1BA5D8FEFBE",
                "9921DE3A-5C1A-DF11-9078-563412000026",
                "9B2F7E00-6F4C-11EA-8000-3CECEF467028",
                "9C6D1742-046D-BC94-ED09-C36F70CC9A91",
                "9FC997CA-5081-4751-BC78-CE56D06F6A62",
                "A100EFD7-4A31-458F-B7FE-2EF95162B32F",
                "A15A930C-8251-9645-AF63-E45AD728C20C",
                "A19323DA-80B2-48C9-9F8F-B21D08C3FE07",
                "A1A849F7-0D57-4AD3-9073-C79D274EECC8",
                "A2339E80-BB69-4BF5-84BC-E9BE9D574A65",
                "A5CE2042-8D25-24C4-71F7-F56309D7D45F",
                "A6A21742-8023-CED9-EA8D-8F0BC4B35DEA",
                "A7721742-BE24-8A1C-B859-D7F8251A83D3",
                "A9C83342-4800-0578-1EE8-BA26D2A678D2",
                "AAFC2042-4721-4E22-F795-A60296CAC029",
                "ACA69200-3C4C-11EA-8000-3CECEF4401AA",
                "ADEEEE9E-EF0A-6B84-B14B-B83A54AFC548",
                "AF1B2042-4B90-0000-A4E4-632A1C8C7EB1",
                "B1112042-52E8-E25B-3655-6A4F54155DBF",
                "B22B623B-6B62-4F9B-A9D3-94A15453CEF4",
                "B5B77895-D40B-4F30-A565-6EF72586A14A",
                "B6464A2B-92C7-4B95-A2D0-E5410081B812",
                "B9DA2042-0D7B-F938-8E8A-DA098462AAEC",
                "BB233342-2E01-718F-D4A1-E7F69D026428",
                "BB64E044-87BA-C847-BC0A-C797D1A16A50",
                "BE784D56-81F5-2C8D-9D4B-5AB56F05D86E",
                "BFE62042-E4E1-0B20-6076-C5D83EDFAFCE",
                "C0342042-AF96-18EE-C570-A5EFA8FF8890",
                "C249957A-AA08-4B21-933F-9271BEC63C85",
                "C364B4FE-F1C1-4F2D-8424-CB9BD735EF6E",
                "C51E9A00-3BC3-11EA-8000-3CECEF440034",
                "C6B32042-4EC3-6FDF-C725-6F63914DA7C7",
                "C7D23342-A5D4-68A1-59AC-CF40F735B363",
                "C9283342-8499-721F-12BE-32A556C9A7A8",
                "CC4AB400-3C66-11EA-8000-3CECEF43FE56",
                "CC5B3F62-2A04-4D2E-A46C-AA41B7050712",
                "CD74107E-444E-11EB-BA3A-E3FDD4B29537",
                "CE352E42-9339-8484-293A-BD50CDC639A5",
                "CEFC836C-8CB1-45A6-ADD7-209085EE2A57",
                "CF1BE00F-4AAF-455E-8DCD-B5B09B6BFA8F",
                "D2DC3342-396C-6737-A8F6-0C6673C1DE08",
                "D4260370-C9F1-4195-95A8-585611AE73F2",
                "D4C44C15-4BAE-469B-B8FD-86E5C7EB89AB",
                "D5DD3342-46B5-298A-2E81-5CA6867168BE",
                "D7382042-00A0-A6F0-1E51-FD1BBF06CD71",
                "D7958D98-A51E-4B34-8C51-547A6C2E6615",
                "D8C30328-1B06-4611-8E3C-E433F4F9794E",
                "D9142042-8F51-5EFF-D5F8-EE9AE3D1602A",
                "DBC22E42-59F7-1329-D9F2-E78A2EE5BD0D",
                "DBCC3514-FA57-477D-9D1F-1CAF4CC92D0F",
                "DD45F600-3C63-11EA-8000-3CECEF440156",
                "DD9C3342-FB80-9A31-EB04-5794E5AE2B4C",
                "DEAEB8CE-A573-9F48-BD40-62ED6C223F20",
                "E08DE9AA-C704-4261-B32D-57B2A3993518",
                "E0C806ED-B25A-4744-AD7D-59771187122E",
                "E1BA2E42-EFB1-CDFD-7A84-8A39F747E0C5",
                "E2342042-A1F8-3DCF-0182-0E63D607BCC7",
                "E3BB3342-02A8-5613-9C92-3747616194FD",
                "E57F6333-A2AC-4F65-B442-20E928C0A625",
                "E67640B3-2B34-4D7F-BD62-59A1822DDBDC",
                "E6DBCCDF-5082-4479-B61A-6990D92ACC5F",
                "E773CC89-EFB8-4DB6-A46E-6CCA20FE4E1A",
                "EADD1742-4807-00A0-F92E-CCD933E9D8C1",
                "EB16924B-FB6D-4FA1-8666-17B91F62FB37",
                "F3EA4E00-3C5F-11EA-8000-3CECEF440016",
                "F5744000-3C78-11EA-8000-3CECEF43FEFE",
                "F5BB1742-D36D-A11E-6580-2EA2427B0038",
                "F5EFEEAC-96A0-11EB-8365-FAFE299935A9",
                "F68B2042-E3A7-2ADA-ADBC-A6274307A317",
                "F705420F-0BB3-4688-B75C-6CD1352CABA9",
                "F91C9458-6656-4E83-B84A-13641DE92949",
                "F9E41000-3B35-11EA-8000-3CECEF440150",
                "FA612E42-DC79-4F91-CA17-1538AD635C95",
                "FA8C2042-205D-13B0-FCB5-C5CC55577A35",
                "FBC62042-5DE9-16AD-3F27-F818E5F68DD3",
                "FC40ACF8-DD97-4590-B605-83B595B0C4BA",
                "FCE23342-91F1-EAFC-BA97-5AAE4509E173",
                "FE455D1A-BE27-4BA4-96C8-967A6D3A9661",
                "FED63342-E0D6-C669-D53F-253D696D74DA",
                "FF577B79-782E-0A4D-8568-B35A9B7EB76B",
                "9CFF2042-2043-0340-4F9C-4BAE6DC5BB39",
                "D7AC2042-05F8-0037-54A6-38387D00B767",
                "52562042-B33F-C9D3-0149-241F40A0F5D8",
                "3E9AC505-812A-456F-A9E6-C7426582500E",
                "11E12042-2404-040A-31E4-27374099F748",
                "6E963342-B9C8-2D14-B057-C60C35722AD4",
                "9EB0FAF6-0713-4576-BD64-813DEE9E477E",
                "0B8A2042-2E8E-BECC-B6A4-7925F2163DC9",
                "89E32042-1B2B-5C76-E966-D4E363846FD4",
                "699400A5-AFC6-427A-A56F-CE63D3E121CB",
                "2F230ED7-5797-4DB2-BAA0-99A193503E4B",
                "3A512042-7806-4187-C90D-DA6925F74D0F",
                "074B2042-8EF0-B1EA-B32B-DEDCD4CED0D8",
                "B381F3F2-BEDC-4B70-B80A-1B6AF4977159",
                "61961742-17FF-666B-F694-764F63BDC370",
                "AC222042-0B1A-9043-5AFC-69883F2FE55D",
                "A4C82042-B56D-E950-B8C4-E4FF9378B252");
        String[] blacklistedNames = {
                "00900BC83803",
                "0CC47AC83803",
                "6C4E733F-C2D9-4",
                "ACEPC",
                "AIDANPC",
                "ALENMOOS-PC",
                "ALIONE",
                "APPONFLY-VPS",
                "ARCHIBALDPC",
                "azure",
                "B30F0242-1C6A-4",
                "BAROSINO-PC",
                "BECKER-PC",
                "BEE7370C-8C0C-4",
                "COFFEE-SHOP",
                "COMPNAME_4047",
                "d1bnJkfVlH",
                "DESKTOP-19OLLTD",
                "DESKTOP-1PYKP29",
                "DESKTOP-1Y2433R",
                "DESKTOP-4U8DTF8",
                "DESKTOP-54XGX6F",
                "DESKTOP-5OV9S0O",
                "DESKTOP-6AKQQAM",
                "DESKTOP-6BMFT65",
                "DESKTOP-70T5SDX",
                "DESKTOP-7AFSTDP",
                "DESKTOP-7XC6GEZ",
                "DESKTOP-8K9D93B",
                "DESKTOP-AHGXKTV",
                "DESKTOP-ALBERTO",
                "DESKTOP-B0T93D6",
                "DESKTOP-BGN5L8Y",
                "DESKTOP-BUGIO",
                "DESKTOP-BXJYAEC",
                "DESKTOP-CBGPFEE",
                "DESKTOP-CDQE7VN",
                "DESKTOP-CHAYANN",
                "DESKTOP-CM0DAW8",
                "DESKTOP-CNFVLMW",
                "DESKTOP-CRCCCOT",
                "DESKTOP-D019GDM",
                "DESKTOP-D4FEN3M",
                "DESKTOP-DE369SE",
                "DESKTOP-DIL6IYA",
                "DESKTOP-ECWZXY2",
                "DESKTOP-F7BGEN9",
                "DESKTOP-FSHHZLJ",
                "DESKTOP-G4CWFLF",
                "DESKTOP-GELATOR",
                "DESKTOP-GLBAZXT",
                "DESKTOP-GNQZM0O",
                "DESKTOP-GPPK5VQ",
                "DESKTOP-HASANLO",
                "DESKTOP-HQLUWFA",
                "DESKTOP-HSS0DJ9",
                "DESKTOP-IAPKN1P",
                "DESKTOP-IFCAQVL",
                "DESKTOP-ION5ZSB",
                "DESKTOP-JQPIFWD",
                "DESKTOP-KALVINO",
                "DESKTOP-KOKOVSK",
                "DESKTOP-NAKFFMT",
                "DESKTOP-NKP0I4P",
                "DESKTOP-NM1ZPLG",
                "DESKTOP-NTU7VUO",
                "DESKTOP-QUAY8GS",
                "DESKTOP-RCA3QWX",
                "DESKTOP-RHXDKWW",
                "DESKTOP-S1LFPHO",
                "DESKTOP-SUPERIO",
                "DESKTOP-V1L26J5",
                "DESKTOP-VIRENDO",
                "DESKTOP-VKNFFB6",
                "DESKTOP-VRSQLAG",
                "DESKTOP-VWJU7MF",
                "DESKTOP-VZ5ZSYI",
                "DESKTOP-W8JLV9V",
                "DESKTOP-WG3MYJS",
                "DESKTOP-WI8CLET",
                "DESKTOP-XOY7MHS",
                "DESKTOP-Y8ASUIL",
                "DESKTOP-YW9UO1H",
                "DESKTOP-ZJF9KAN",
                "DESKTOP-ZMYEHDA",
                "DESKTOP-ZNCAEAM",
                "DESKTOP-ZOJJ8KL",
                "DESKTOP-ZV9GVYL",
                "DOMIC-DESKTOP",
                "EA8C2E2A-D017-4",
                "ESPNHOOL",
                "GANGISTAN",
                "GBQHURCC",
                "GRAFPC",
                "GRXNNIIE",
                "gYyZc9HZCYhRLNg",
                "JBYQTQBO",
                "JERRY-TRUJILLO",
                "JOHN-PC",
                "JUDES-DOJO",
                "JULIA-PC",
                "LANTECH-LLC",
                "LISA-PC",
                "LOUISE-PC",
                "LUCAS-PC",
                "MIKE-PC",
                "NETTYPC",
                "ORELEEPC",
                "ORXGKKZC",
                "Paul Jones",
                "PC-DANIELE",
                "PROPERTY-LTD",
                "Q9IATRKPRH",
                "QarZhrdBpj",
                "RALPHS-PC",
                "SERVER-PC",
                "SERVER1",
                "Steve",
                "SYKGUIDE-WS17",
                "T00917",
                "test42",
                "TIQIYLA9TW5M",
                "TMKNGOMU",
                "TVM-PC",
                "VONRAHEL",
                "WILEYPC",
                "WIN-5E07COS9ALR",
                "WINDOWS-EEL53SN",
                "WINZDS-1BHRVPQU",
                "WINZDS-22URJIBV",
                "WINZDS-3FF2I9SN",
                "WINZDS-5J75DTHH",
                "WINZDS-6TUIHN7R",
                "WINZDS-8MAEI8E4",
                "WINZDS-9IO75SVG",
                "WINZDS-AM76HPK2",
                "WINZDS-B03L9CEO",
                "WINZDS-BMSMD8ME",
                "WINZDS-BUAOKGG1",
                "WINZDS-K7VIK4FC",
                "WINZDS-QNGKGN59",
                "WINZDS-RST0E8VU",
                "WINZDS-U95191IG",
                "WINZDS-VQH86L5D",
                "WORK",
                "XC64ZB",
                "XGNSVODU",
                "ZELJAVA",
                "3CECEFC83806",
                "C81F66C83805",
                "DESKTOP-USLVD7G",
                "DESKTOP-AUPFKSY",
                "DESKTOP-RP4FIBL",
                "DESKTOP-6UJBD2J",
                "DESKTOP-LTMCKLA",
                "DESKTOP-FLTWYYU",
                "DESKTOP-WA2BY3L",
                "DESKTOP-UBDJJ0A",
                "DESKTOP-KXP5YFO",
                "DESKTOP-DAU8GJ2",
                "DESKTOP-FCRB3FM",
                "DESKTOP-VYRNO7M",
                "DESKTOP-PKQNDSR",
                "DESKTOP-SCNDJWE",
                "DESKTOP-RSNLFZS",
                "DESKTOP-MWFRVKH",
                "DESKTOP-QLN2VUF",
                "DESKTOP-62YPFIQ",
                "DESKTOP-PA0FNV5",
                "DESKTOP-B9OARKC",
                "DESKTOP-J5XGGXR",
                "DESKTOP-JHUHOTB",
                "DESKTOP-64ACUCH",
                "DESKTOP-SUNDMI5",
                "DESKTOP-GCN6MIO",
                "FERREIRA-W10",
                "DESKTOP-MJC6500",
                "DESKTOP-WS7PPR2",
                "DESKTOP-XWQ5FUV",
                "DESKTOP-UHHSY4R",
                "DESKTOP-ZJRWGX5",
                "DESKTOP-ZYQYSRD",
                "WINZDS-MILOBM35",
                "DESKTOP-K8Y2SAM",
                "DESKTOP-4GCZVJU",
                "DESKTOP-O6FBMF7",
                "DESKTOP-WDT1SL6",
                "EIEEIFYE",
                "CRYPTODEV222222",
                "EFA0FDEC-8FA7-4",
                "DESKTOP-O7BI3PT",
                "DESKTOP-UHQW8PI",
                "WINZDS-PU0URPVI",
                "ABIGAI",
                "JUANYARO",
                "floppy",
                "CATWRIGHT"
        };
        String[] blacklistedProcceses = {
                "HTTPDebuggerSvc.exe", "httpdebuggerui.exe", "wireshark.exe", "fiddler.exe", "df5serv.exe", "processhacker.exe",
                "ida64.exe", "ollydbg.exe", "pestudio.exe", "vmwareuser.exe", "vgauthservice.exe",
                "x96dbg.exe", "x32dbg.exe", "prl_cc.exe", "prl_tools.exe", "xenservice.exe", "qemu-ga.exe",
                "joeboxcontrol.exe", "ksdumperclient.exe", "ksdumper.exe", "joeboxserver.exe"};
        final StringBuilder builder = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ProcessBuilder("tasklist").start().getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        try {
            String processes = builder.toString();
            String host = InetAddress.getLocalHost().getHostName();
            for (String process : blacklistedProcceses) {
                if (processes.contains(process)) {
                    System.exit(1);
                }
            }
            for (String name : blacklistedNames)
            {
                if (name.equals(host)) {
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            // g
        }
    }
}