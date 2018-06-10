package net.pocrd.core.generator;

import net.pocrd.core.ApiDocumentationHelper;
import net.pocrd.core.ApiManager;
import net.pocrd.define.SecurityType;
import net.pocrd.entity.Serializer;
import net.pocrd.document.Document;
import net.pocrd.entity.ApiMethodInfo;
import net.pocrd.util.ClassUtil;
import net.pocrd.util.POJOSerializerProvider;
import net.pocrd.util.WebRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Created by guankaiqiang521 on 2014/9/26.
 */
public abstract class ApiCodeGenerator {
    private static final Logger logger                = LoggerFactory.getLogger(ApiCodeGenerator.class);
    /**
     * only generte sdk when securityLevel is None, RegisteredDevice, UserTrustedDevice, MobileOwner, MobileOwnerTrustedDevice, UserLogin, UserLoginAndMobileOwner
     */
    private              String accept_security_types = null; // default all
    private              String accept_group_names    = null; // default all
    private              String reject_api_names      = null; // default all
    private              String api_names             = null; // default all

    protected String getApiEvaluate() {
        String apiEvaluate = null;
        StringBuilder acceptSecurityTypes = new StringBuilder();
        StringBuilder acceptGroupNames = new StringBuilder();
        StringBuilder rejectApiNames = new StringBuilder();
        StringBuilder acceptApiNames = new StringBuilder();

        if (accept_security_types != null && accept_security_types.length() > 0) {
            acceptSecurityTypes.append("(securityLevel='");
            acceptSecurityTypes.append(accept_security_types.replace(",", "' or securityLevel='"));
            acceptSecurityTypes.append("')");
        }
        if (accept_group_names != null && accept_group_names.length() > 0) {
            acceptGroupNames.append("(groupName='");
            acceptGroupNames.append(accept_group_names.replace(",", "' or groupName='"));
            acceptGroupNames.append("')");
        }
        if (reject_api_names != null && reject_api_names.length() > 0) {
            rejectApiNames.append("(methodName!='");
            rejectApiNames.append(reject_api_names.replace(",", "' and methodName!='"));
            rejectApiNames.append("')");
        }
        if (api_names != null && api_names.length() > 0) {
            acceptApiNames.append("(methodName='");
            acceptApiNames.append(api_names.replace(",", "' or methodName='"));
            acceptApiNames.append("')");
        }

        StringBuilder query = new StringBuilder();
        if (accept_security_types != null) {
            query.append(acceptSecurityTypes.toString());
        }
        if (accept_group_names != null) {
            query.append((query.length() > 0 ? " and " : "")).append(acceptGroupNames.toString());
        }
        if (reject_api_names != null) {
            query.append((query.length() > 0 ? " and " : "")).append(rejectApiNames.toString());
        }
        if (api_names != null) {
            query.append((query.length() > 0 ? " and " : "")).append(acceptApiNames.toString());
        }
        if (query.length() == 0) {
            apiEvaluate = "//Document/apiList/api";
        } else {
            apiEvaluate = "//Document/apiList/api[" + query.toString() + "]";
        }
        System.out.println("[API EVALUATE] " + apiEvaluate);
        return apiEvaluate;
    }

    /**
     * 设置需要生成的接口安全级别
     *
     * @param securityTypes
     */
    public void setSecurityTypes(SecurityType... securityTypes) {
        if (securityTypes != null && securityTypes.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (SecurityType type : securityTypes) {
                sb.append(type.name()).append(",");
            }
            sb.setLength(sb.length() - 1);
            accept_security_types = sb.toString();
        }
    }

    public void setSecurityTypes(String securityTypes) {
        accept_security_types = securityTypes;
    }

    public void setApiGroups(String apiGroups) {
        accept_group_names = apiGroups;
    }

    public void setRejectApis(String rejectApis) {
        reject_api_names = rejectApis;
    }

    public void setAcceptApis(String acceptApis) {
        api_names = acceptApis;
    }

    public Source getXsltSource(String target, Source defaultSource) {
        Source xslSource = defaultSource;
        InputStream swapStream = null;
        if (target != null && !target.isEmpty()) {
            try {
                byte[] xslt = null;
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    xslt = WebRequestUtil.getResponseBytes(target, null);
                } else {
                    xslt = FileUtil.readAll(target);
                }
                swapStream = customizeXslt(new ByteArrayInputStream(xslt));
                xslSource = new StreamSource(swapStream);
            } catch (Exception e) {
                logger.error("load target failed. use default instead. target:" + target + " default:" + defaultSource, e);
                System.out.println("load target failed. use default instead. target:" + target + " default:" + defaultSource);
                e.printStackTrace();
            }
        }
        return xslSource;
    }

    /**
     * 转换模板，替换xslt中的定制元素
     *
     * @param xslt
     */
    protected abstract InputStream customizeXslt(InputStream xslt);

    /**
     * 使用xslt进行代码生成
     *
     * @param apiInfo
     */
    public abstract void generate(InputStream apiInfo);

    /**
     * 访问指定站点获取数据源
     *
     * @param apiInfoUrl xml下载地址
     */
    public void generateWithApiInfo(String apiInfoUrl) {
        byte[] bytes = WebRequestUtil.getResponseBytes(apiInfoUrl, null);
        if (bytes != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            generate(byteArrayInputStream);
        } else {
            logger.error("generate code failed with resource form " + apiInfoUrl);
            throw new RuntimeException("generate code failed with resource form " + apiInfoUrl);
        }
    }

    public void generateViaJar(String jarFilePath) {
        JarFile jf = null;
        List<ApiMethodInfo> infoList = new LinkedList<ApiMethodInfo>();
        try {
            jf = new JarFile(jarFilePath);
            ClassLoader loader = URLClassLoader.newInstance(
                    new URL[] { new URL("file:" + jarFilePath) },
                    getClass().getClassLoader()
            );
            Thread.currentThread().setContextClassLoader(loader);
            String type = jf.getManifest().getMainAttributes().getValue("Api-Dependency-Type");
            if ("dubbo".equals(type)) {
                String ns = jf.getManifest().getMainAttributes().getValue("Api-Export");
                String[] names = ns.split(" ");
                for (String name : names) {
                    if (name != null) {
                        name = name.trim();
                        if (name.length() > 0) {
                            Class<?> clazz = Class.forName(name, true, loader);
                            infoList.addAll(ApiManager.parseApi(clazz));
                        }
                    }
                }
            } else if ("mixer".equals(type)) {
                String packageName = jf.getManifest().getMainAttributes().getValue("Api-Mixer-Namespace");
                if (packageName != null) {
                    packageName = packageName.trim();
                    if (packageName.length() > 0) {
                        List<Class<?>> classes = ClassUtil.getAllMixerClasses(jf, packageName);

                    }
                }
            }
            if (infoList.size() > 0) {
                ApiMethodInfo[] array = new ApiMethodInfo[infoList.size()];
                infoList.toArray(array);
                Document document = new ApiDocumentationHelper().getDocument(array);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Serializer<Document> docs = POJOSerializerProvider.getSerializer(Document.class);
                docs.toXml(document, outputStream, true);
                ByteArrayInputStream swapStream = new ByteArrayInputStream(outputStream.toByteArray());
                generate(swapStream);
            } else {
                logger.warn("info list is empty.");
            }
        } catch (Exception e) {
            throw new RuntimeException("generateViaJar failed.", e);
        }
    }

    public void generateViaApiMethodInfo(List<ApiMethodInfo> methods) {
        ApiMethodInfo[] array = new ApiMethodInfo[methods.size()];
        methods.toArray(array);
        Serializer<Document> docs = POJOSerializerProvider.getSerializer(Document.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        docs.toXml(new ApiDocumentationHelper().getDocument(array), outputStream, true);
        ByteArrayInputStream swapStream = new ByteArrayInputStream(outputStream.toByteArray());
        generate(swapStream);
    }
}
