class AutocrudBuilder extends BuilderSupport {
    @Override
    protected void setParent(Object parent, Object child) {
        var children = parent[child.type as String]
        if (!children) {
            children = []
            parent[child.type as String] = children
        }
        children << child
    }

    @Override
    protected Object createNode(Object name) {
        createNode(name, null)
    }

    @Override
    protected Object createNode(Object name, Object value) {
        createNode(name, [] as HashMap, value)
    }

    @Override
    protected Object createNode(Object name, Map attributes) {
        createNode(name, attributes, null)
    }

    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        [type: name] << (attributes ? attributes : [] as Map)
    }
}


var define = new AutocrudBuilder().define {
    field(name: 'username', title: '用户名')
    field(name: 'password', title: '密码')
    field(name: 'mobile', title: '手机号')
    field(name: 'email', title: '邮箱')
    field(name: 'locked', title: '是否锁定')
    field(name: 'createdAt', title: '创建时间')
    field(name: 'updatedAt', title: '更新时间')

    insert(name: 'add', api: '/api/user/add') {
        field(name: 'username', nonEmpty: true)
        field(name: 'password', nonEmpty: true)
        field(name: 'mobile')
        field(name: 'email')
    }
    update(name: 'setProfile', api: '/api/user/setProfile', findBy: 'findById') {
        field(name: 'mobile')
        field(name: 'email')
    }
    update(name: 'setLocked', api: '/api/user/setLocked', findBy: 'findById') {
        field(name: 'locked')
    }
    select(name: 'getDetails', api: '/api/user/getDetails', findBy: 'findById') {
        field(name: 'username')
        field(name: 'password')
        field(name: 'mobile')
        field(name: 'email')
        field(name: 'locked')
        field(name: 'createdAt')
        field(name: 'updatedAt')
    }
}
println(define)