## Extensions Security

Extensions Developers need clearly documented and simple methods for providing security in extensions. The extensions SDK
aims to provide a framework for secure extensions development to make extensions secure by default, a break from the
current plugin model where a security plugin must be installed to obtain security features. When extensions developers
develop functionality for extensions, they need a means for:

- Defining permissions
- Defining roles
- Checking user permissions

### Definite permissions

- TODO How should permissions be defined?

Hypothetical example of permissions:

```yaml
# permissions.yml

# Example of named permission
EditDetectorWithConstraints:
  action: EditDetector           # Action would be defined by an extension developer
  description: Edit detectors with restrictions
  constraints:
    - resource_name: *           # [NamePattern]
    - index_patterns:
      - ad*                      # [IndexPattern]
    - created_by:
      - user1
      - user2
    - created_at: ">=2000-01-01" # [DatetimeExpression]

EditOwnDetector:
  action: EditDetector
  description: Edit detector owned by current user
  constraints:
    created_by:
      - "${user_name}"
  
# TODO Are some constraints required and others optional?
```

See the section on `Checking user permissions` to see how the permissions can be checked on the handling of an AuthorizationRequest.

### Defining roles

- TODO How should roles be defined?

Hypothetical example of roles:

```yaml
# roles.yml

DetectorResultsRO:
  permissions:
    - action: CreateDetector
      description: Create detector
    - action: ListResults
      description: List results
    - action: CreateResults
      description: Create detector results
  named_permissions:
    - EditOwnDetector
```

This is an example of how a plugin defines roles currently:

```yaml
# Allow users to read Anomaly Detection detectors and results
anomaly_read_access:
  reserved: true
  cluster_permissions:
    - 'cluster:admin/opendistro/ad/detector/info'
    - 'cluster:admin/opendistro/ad/detector/search'
    - 'cluster:admin/opendistro/ad/detectors/get'
    - 'cluster:admin/opendistro/ad/result/search'
    - 'cluster:admin/opendistro/ad/tasks/search'
    - 'cluster:admin/opendistro/ad/detector/validate'
    - 'cluster:admin/opendistro/ad/result/topAnomalies'

# Allows users to use all Anomaly Detection functionality
anomaly_full_access:
  reserved: true
  cluster_permissions:
    - 'cluster_monitor'
    - 'cluster:admin/opendistro/ad/*'
  index_permissions:
    - index_patterns:
        - '*'
      allowed_actions:
        - 'indices_monitor'
        - 'indices:admin/aliases/get'
        - 'indices:admin/mappings/get'
```

### Checking user permissions

Before any action is performed on an extension, the action should be preceded with an `AuthorizationRequest` to 
determine whether the user of an extension has the required privileges to perform the action. An authorization
request consists of the name of a permission, a user identifier and a map of required parameters needed to check if a 
user has the required permissions. AuthorizationRequest handling is performed in core and yet to be implemented.

```java
public class AuthorizationRequest extends TransportRequest {
    private String extensionUniqueId;
    private PrincipalIdentifierToken requestIssuerIdentity;
    private String permissionId;
    private Map<String, Object> params;

    public AuthorizationRequest(String extensionUniqueId, PrincipalIdentifierToken requestIssuerIdentity, String permissionId, Map<String, Object> params) {
        this.extensionUniqueId = extensionUniqueId;
        this.requestIssuerIdentity = requestIssuerIdentity;
        this.permissionId = permissionId;
        this.params = params;
    }
}
```

The `params` sent with an authorization request are _checkable_ and need to fit in constraints defined in the permissions.

Questions:

- Does this capture all of the information needed to perform an authorization request?

### Example Extension - Building Management and Keycard Access

For an example of security features for an extension imagine a Property Management and Keycard Access extension. This 
extension will be used by property managers to register properties, grant and revoke access to tenants and to
monitor for suspicious activity. Tenants will indirectly use the extension when scanning keycards at different points 
to determine whether they have access to a building, floor or door. 

Example permissions requests:

    - Am I allowed to register a new property?
    - Can I enter this building? Can Peter enter this building?
    - Do I have access to a specific floor/door in a building?
    - Can I list all of the registered properties?
    - Can I view a dashboard of a count of failed authorization requests?

Various Actions include:
    - RegisterProperty - Used by a property manager to add properties to the system
    - GrantAccess - Used by a property manager to grant access to tenants
    - RevokeAccess - Used by a property manager to revoke access to tenants
    - AccessBuilding - Used by any keycard holder requesting access to a building
    - AccessFloor - Used by any keycard holder requesting access to a floor
    - AccessDoor - Used by any keycard holder requesting access to a door

```yaml
# permissions.yml

# Example of named permission
BillingDepartmentFloorAccess:
  action: AccessFloor
  description: Permission required to enter the floors of the billing deparmtne
  constraints:
    - resource_name:
        - 5
        - 6

OwnOffice:
  action: AccessDoor
  description: Access any door where current user is a tenant
  constraints:
    tenant:
      - "${user_name}"
```

```yaml
# roles.yml

BuildingSuper:
  permissions:
    - action: AccessBuilding
      description: Access to all buildings
      constraints:
        resource_id: *
    - action: AccessFloor
      description: Access to all floors
      constraints:
        resource_id: *
    - action: AccessDoor
      description: Access to all doors
      constraints:
        resource_id: *
```
