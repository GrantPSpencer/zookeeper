/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.annotations.VisibleForTesting;
import org.apache.zookeeper.server.auth.znode.groupacl.X509ZNodeGroupAclProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains config properties for x509-based authentication
 */
public class X509AuthenticationConfig {
  private static final Logger LOG = LoggerFactory.getLogger(X509AuthenticationConfig.class);
  private static X509AuthenticationConfig instance = null;
  private static final String LOG_MSG_PREFIX = X509AuthenticationConfig.class.getName() + "::";

  private X509AuthenticationConfig() {
  }

  public static X509AuthenticationConfig getInstance() {
    if (instance == null) {
      synchronized (X509AuthenticationConfig.class) {
        if (instance == null) {
          instance = new X509AuthenticationConfig();
        }
      }
    }
    return instance;
  }

  // The following System Property keys are used to extract clientId from the client cert.

  /**
   * Config prefix for x509-related config properties.
   * The property keys start with this prefix apply to both {@link org.apache.zookeeper.server.auth.X509AuthenticationProvider}
   * and {@link org.apache.zookeeper.server.auth.znode.groupacl.X509ZNodeGroupAclProvider}
   */
  public static final String SSL_X509_CONFIG_PREFIX = "zookeeper.ssl.x509.";
  /**
   * Determines which field in the x509 certificate to be used for client Id:
   * SAN (subject alternative name) or SDN (subject domain name) (default)
   */
  public static final String SSL_X509_CLIENT_CERT_ID_TYPE =
      SSL_X509_CONFIG_PREFIX + "clientCertIdType";
  /** The field in SAN to be used for client URI extraction */
  public static final String SSL_X509_CLIENT_CERT_ID_SAN_MATCH_TYPE =
      SSL_X509_CONFIG_PREFIX + "clientCertIdSanMatchType";
  /** Match Regex is used to choose which entry to use, default value ".*" */
  public static final String SSL_X509_CLIENT_CERT_ID_SAN_MATCH_REGEX =
      SSL_X509_CONFIG_PREFIX + "clientCertIdSanMatchRegex";
  /** Extract Regex is used to construct a client ID (acl entity) to return, default value ".*" */
  public static final String SSL_X509_CLIENT_CERT_ID_SAN_EXTRACT_REGEX =
      SSL_X509_CONFIG_PREFIX + "clientCertIdSanExtractRegex";
  /** Specifies match group index for the extract regex (i in Matcher.group(i)), default value 0 */
  public static final String SSL_X509_CLIENT_CERT_ID_SAN_EXTRACT_MATCHER_GROUP_INDEX =
      SSL_X509_CONFIG_PREFIX + "clientCertIdSanExtractMatcherGroupIndex";
  public static final String SUBJECT_ALTERNATIVE_NAME_SHORT = "SAN";
  private static final String DEFAULT_REGEX = ".*";
  private String clientCertIdType;
  private int clientCertIdSanMatchType = -1;
  private String clientCertIdSanMatchRegex;
  private String clientCertIdSanExtractRegex;
  private int clientCertIdSanExtractMatcherGroupIndex = -1;

  // ZooKeeper server-side config properties for ZNode group ACL feature

  /**
   * Config prefix for znode group acl-related config properties
   */
  public static final String ZNODE_GROUP_ACL_CONFIG_PREFIX = "zookeeper.X509ZNodeGroupAclProvider.";

  /**
   * Enables/disables whether znodes created by auth'ed clients.
   * Should be enabled if znode group acl feature is desired; otherwise, it should be disabled.
   * When this property is enabled:
   *    1. Should have ACL fields populated with the client Id given by the authentication provider.
   *    2. Users do not have the right to manually operate on znode ACLs
   *    3. {@value ZOOKEEPER_ZNODEGROUPACL_SUPERUSER_ID} has the ability to operate on znode ACLs
   * Has the same effect as the ZK client using ZooDefs.Ids.CREATOR_ALL_ACL.
   */
  public static final String SET_X509_CLIENT_ID_AS_ACL =
      ZNODE_GROUP_ACL_CONFIG_PREFIX + "setX509ClientIdAsAcl";
  /**
   * A list of domain names that will have cross-domain access privilege, separated by ","
   */
  public static final String CROSS_DOMAIN_ACCESS_DOMAIN_NAME =
      ZNODE_GROUP_ACL_CONFIG_PREFIX + "crossDomainAccessDomainName";
  /**
   * A user-defined URI represents a super user, same concept as the original x509 superuser.
   * Super user can operate on the znode to set ACLs.
   */
  public static final String ZOOKEEPER_ZNODEGROUPACL_SUPERUSER_ID =
      ZNODE_GROUP_ACL_CONFIG_PREFIX + "superUserId";
  /**
   * A list of znode path prefixes, separated by ",".
   * Znode whose path starts with the defined path prefix would have open read access.
   * Meaning the znode will have (world:anyone, r) ACL.
   */
  public static final String OPEN_READ_ACCESS_PATH_PREFIX =
      ZNODE_GROUP_ACL_CONFIG_PREFIX + "openReadAccessPathPrefix";
  /**
   * If the server is dedicated for one domain, use this config property to define the domain name,
   * and enable connection filtering feature for this domain.
   */
  public static final String DEDICATED_DOMAIN = ZNODE_GROUP_ACL_CONFIG_PREFIX + "dedicatedDomain";
  /**
   * The root path for client URI - domain mapping that stored in zk data tree.
   * Refer to {@link org.apache.zookeeper.server.auth.znode.groupacl.ZkClientUriDomainMappingHelper}
   * for details about client URI - domain mapping.
   */
  public static final String CLIENT_URI_DOMAIN_MAPPING_ROOT_PATH =
      ZNODE_GROUP_ACL_CONFIG_PREFIX + "clientUriDomainMappingRootPath";

  private String x509ClientIdAsAclEnabled;
  private String znodeGroupAclSuperUserId;
  private String znodeGroupAclCrossDomainAccessDomainNameStr;
  private String znodeGroupAclOpenReadAccessPathPrefixStr;
  private String znodeGroupAclServerDedicatedDomain;
  private String znodeGroupAclClientUriDomainMappingRootPath;
  // Although using "volatile" keyword with double checked locking could prevent the undesired
  //creation of multiple objects; not using here for the consideration of read performance
  private Set<String> openReadAccessPathPrefixes;
  private Set<String> crossDomainAccessDomains;
  private final Object openReadAccessPathPrefixesLock = new Object();
  private final Object crossDomainAccessDomainsLock = new Object();

  // Setters for X509 properties

  public void setClientCertIdType(String clientCertIdType) {
    this.clientCertIdType = clientCertIdType;
  }

  public void setClientCertIdSanMatchType(String clientCertIdSanMatchType) {
    if (clientCertIdSanMatchType == null) {
      return;
    }
    try {
      this.clientCertIdSanMatchType = Integer.parseInt(clientCertIdSanMatchType);
    } catch (NumberFormatException e) {
      String errMsg =
          LOG_MSG_PREFIX + "Could not parse number for clientCertIdSanMatchType, provided value: "
              + clientCertIdSanMatchType;
      LOG.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }
  }

  public void setClientCertIdSanMatchRegex(String clientCertIdSanMatchRegex) {
    this.clientCertIdSanMatchRegex = clientCertIdSanMatchRegex;
  }

  public void setClientCertIdSanExtractRegex(String clientCertIdSanExtractRegex) {
    this.clientCertIdSanExtractRegex = clientCertIdSanExtractRegex;
  }

  public void setClientCertIdSanExtractMatcherGroupIndex(
      String clientCertIdSanExtractMatcherGroupIndex) {
    if (clientCertIdSanExtractMatcherGroupIndex == null) {
      return;
    }
    try {
      this.clientCertIdSanExtractMatcherGroupIndex =
          Integer.parseInt(clientCertIdSanExtractMatcherGroupIndex);
    } catch (NumberFormatException e) {
      String errMsg = LOG_MSG_PREFIX
          + "Could not parse number for clientCertIdSanExtractMatcherGroupIndex, provided value: "
          + clientCertIdSanExtractMatcherGroupIndex;
      LOG.error(errMsg);
      throw new IllegalArgumentException(errMsg);
    }
  }

  // Setters for X509 Znode Group Acl properties

  public void setX509ClientIdAsAclEnabled(String enabled) {
    x509ClientIdAsAclEnabled = enabled;
  }

  public void setZnodeGroupAclSuperUserId(String znodeGroupAclSuperUserId) {
    this.znodeGroupAclSuperUserId = znodeGroupAclSuperUserId;
  }

  public void setZnodeGroupAclCrossDomainAccessDomainNameStr(
      String znodeGroupAclCrossDomainAccessDomainNameStr) {
    this.znodeGroupAclCrossDomainAccessDomainNameStr = znodeGroupAclCrossDomainAccessDomainNameStr;
  }

  public void setZnodeGroupAclOpenReadAccessPathPrefixStr(
      String znodeGroupAclOpenReadAccessPathPrefixStr) {
    this.znodeGroupAclOpenReadAccessPathPrefixStr = znodeGroupAclOpenReadAccessPathPrefixStr;
  }

  public void setZnodeGroupAclServerDedicatedDomain(String znodeGroupAclServerDedicatedDomain) {
    this.znodeGroupAclServerDedicatedDomain = znodeGroupAclServerDedicatedDomain;
  }

  public void setZnodeGroupAclClientUriDomainMappingRootPath(
      String znodeGroupAclClientUriDomainMappingRootPath) {
    this.znodeGroupAclClientUriDomainMappingRootPath = znodeGroupAclClientUriDomainMappingRootPath;
  }

  // Getters for X509 properties

  public String getClientCertIdType() {
    if (clientCertIdType == null) {
      setClientCertIdType(System.getProperty(SSL_X509_CLIENT_CERT_ID_TYPE));
    }
    return clientCertIdType;
  }

  public int getClientCertIdSanMatchType() {
    if (clientCertIdSanMatchType == -1) {
      setClientCertIdSanMatchType(System.getProperty(SSL_X509_CLIENT_CERT_ID_SAN_MATCH_TYPE));
    }
    return clientCertIdSanMatchType;
  }

  public String getClientCertIdSanMatchRegex() {
    if (clientCertIdSanMatchRegex == null) {
      setClientCertIdSanMatchRegex(System.getProperty(SSL_X509_CLIENT_CERT_ID_SAN_MATCH_REGEX));
    }
    return clientCertIdSanMatchRegex == null ? DEFAULT_REGEX : clientCertIdSanMatchRegex;
  }

  public String getClientCertIdSanExtractRegex() {
    if (clientCertIdSanExtractRegex == null) {
      setClientCertIdSanExtractRegex(System.getProperty(SSL_X509_CLIENT_CERT_ID_SAN_EXTRACT_REGEX));
    }
    return clientCertIdSanExtractRegex == null ? DEFAULT_REGEX : clientCertIdSanExtractRegex;
  }

  public int getClientCertIdSanExtractMatcherGroupIndex() {
    if (clientCertIdSanExtractMatcherGroupIndex == -1) {
      setClientCertIdSanExtractMatcherGroupIndex(
          System.getProperty(SSL_X509_CLIENT_CERT_ID_SAN_EXTRACT_MATCHER_GROUP_INDEX));
    }
    return clientCertIdSanExtractMatcherGroupIndex == -1 ? 0
        : clientCertIdSanExtractMatcherGroupIndex;
  }

  // Getters for X509 Znode Group Acl properties

  public boolean isX509ClientIdAsAclEnabled() {
    return Boolean.parseBoolean(x509ClientIdAsAclEnabled) || Boolean
        .parseBoolean(System.getProperty(SET_X509_CLIENT_ID_AS_ACL));
  }

  public String getZnodeGroupAclSuperUserId() {
    if (znodeGroupAclSuperUserId == null) {
      setZnodeGroupAclSuperUserId(System.getProperty(ZOOKEEPER_ZNODEGROUPACL_SUPERUSER_ID));
    }
    return znodeGroupAclSuperUserId;
  }

  public Set<String> getZnodeGroupAclCrossDomainAccessDomains() {
    if (crossDomainAccessDomains == null) {
      synchronized (crossDomainAccessDomainsLock) {
        if (crossDomainAccessDomains == null) {
          crossDomainAccessDomains = loadCrossDomainAccessDomainNames();
        }
      }
    }
    return crossDomainAccessDomains;
  }

  public Set<String> getZnodeGroupAclOpenReadAccessPathPrefixes() {
    if (openReadAccessPathPrefixes == null) {
      synchronized (openReadAccessPathPrefixesLock) {
        if (openReadAccessPathPrefixes == null) {
          openReadAccessPathPrefixes = loadOpenReadAccessPathPrefixes();
        }
      }
    }
    return openReadAccessPathPrefixes;
  }

  public String getZnodeGroupAclServerDedicatedDomain() {
    if (znodeGroupAclServerDedicatedDomain == null) {
      setZnodeGroupAclServerDedicatedDomain(System.getProperty(DEDICATED_DOMAIN));
    }
    return znodeGroupAclServerDedicatedDomain;
  }

  public String getZnodeGroupAclClientUriDomainMappingRootPath() {
    if (znodeGroupAclClientUriDomainMappingRootPath == null) {
      setZnodeGroupAclClientUriDomainMappingRootPath(
          System.getProperty(CLIENT_URI_DOMAIN_MAPPING_ROOT_PATH));
    }
    return znodeGroupAclClientUriDomainMappingRootPath;
  }

  /**
   * Helper method for Znode Group Acl feature
   * Get open read access path prefixes from config
   * @return A set of path prefixes
   */
  private Set<String> loadOpenReadAccessPathPrefixes() {
    if (znodeGroupAclOpenReadAccessPathPrefixStr == null) {
      setZnodeGroupAclOpenReadAccessPathPrefixStr(System.getProperty(OPEN_READ_ACCESS_PATH_PREFIX));
    }
    if (znodeGroupAclOpenReadAccessPathPrefixStr == null
        || znodeGroupAclOpenReadAccessPathPrefixStr.isEmpty()) {
      return Collections.emptySet();
    }
    return Arrays.stream(znodeGroupAclOpenReadAccessPathPrefixStr.split(","))
        .filter(str -> str.length() > 0).collect(Collectors.toSet());
  }

  /**
   * Helper method for Znode Group Acl feature
   * Get the domain names that are mapped to cross-domain access privilege
   * @return A set of domain names
   */
  private Set<String> loadCrossDomainAccessDomainNames() {
    if (znodeGroupAclCrossDomainAccessDomainNameStr == null) {
      setZnodeGroupAclCrossDomainAccessDomainNameStr(System.getProperty(CROSS_DOMAIN_ACCESS_DOMAIN_NAME));
    }
    if (znodeGroupAclCrossDomainAccessDomainNameStr == null || znodeGroupAclCrossDomainAccessDomainNameStr
        .isEmpty()) {
      return Collections.emptySet();
    }
    return Arrays.stream(znodeGroupAclCrossDomainAccessDomainNameStr.split(","))
        .filter(str -> str.length() > 0).collect(Collectors.toSet());
  }

  /**
   * Check if server dedicated domain config property is set
   * This check is only meaningful when x509 znode group acl feature is enabled
   * @return true if a domain is set as the server's dedicated domain; false if not set
   */
  public boolean isZnodeGroupAclDedicatedServerEnabled() {
    return isX509ClientIdAsAclEnabled() && getZnodeGroupAclServerDedicatedDomain() != null
        && !getZnodeGroupAclServerDedicatedDomain().isEmpty();
  }

  /**
   * Check if x509 znode group acl feature is enabled
   * @return true if enabled; false if not
   */
  public boolean isX509ZnodeGroupAclEnabled() {
    return ProviderRegistry
        .getServerProvider(X509AuthenticationUtil.X509_SCHEME) instanceof X509ZNodeGroupAclProvider;
  }

  @VisibleForTesting
  public static void reset() {
    synchronized (X509AuthenticationConfig.class) {
      instance = null;
    }
  }
}