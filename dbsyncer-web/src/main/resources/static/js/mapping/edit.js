//*********************************** 驱动保存 开始位置***********************************//
function submit(data) {
    doPoster("/mapping/edit", data, function (data) {
        if (data.success == true) {
            bootGrowl("修改驱动成功!", "success");
            backIndexPage();
        } else {
            bootGrowl(data.resultValue, "danger");
        }
    });
}
//*********************************** 驱动保存 结束位置***********************************//
// 刷新页面
function refresh(id){
    doLoader('/mapping/page/edit?id=' + id);
}

// 绑定修改驱动同步方式切换事件
function bindMappingModelChange() {
    var $mappingModelChange = $("#mappingModelChange");
    var $radio = $mappingModelChange.find('input:radio[type="radio"]');
    // 初始化icheck插件
    $radio.iCheck({
        labelHover: false,
        cursor: true,
        radioClass: 'iradio_flat-blue',
    }).on('ifChecked', function (event) {
        var $form = $("#mappingModifyForm");
        if ($form.formValidate() == true) {
            var data = $form.serializeJson();
            doPoster("/mapping/edit", data, function (response) {
                if (response.success == true) {
                    refresh($("#mappingId").val());
                } else {
                    bootGrowl(response.resultValue, "danger");
                }
            });
        }
    });

    // 渲染选择radio配置
    var $value = $mappingModelChange.find('input[type="radio"]:checked').val();
    // 显示驱动编辑配置（全量/增量）
    var $full = $("#mappingFullConfig");
    var $increment = $("#mappingIncrementConfig");
    if ('full' == $value) {
        $increment.addClass("hidden");
        $full.removeClass("hidden");
    } else {
        $full.addClass("hidden");
        $increment.removeClass("hidden");
    }
}

// 绑定删除表关系复选框事件
function bindMappingTableGroupCheckBoxClick(){
    var $checkboxAll = $('.tableGroupCheckboxAll');
    var $checkbox = $('.tableGroupCheckbox');
    var $delBtn = $("#tableGroupDelBtn");
    $checkboxAll.iCheck({
        checkboxClass: 'icheckbox_square-red',
        labelHover: false,
        cursor: true
    }).on('ifChecked', function (event) {
        $checkbox.iCheck('check');
    }).on('ifUnchecked', function (event) {
        $checkbox.iCheck('uncheck');
    }).on('ifChanged', function (event) {
        $delBtn.prop('disabled', getCheckedBoxSize($checkbox).length < 1);
    });

    // 初始化icheck插件
    $checkbox.iCheck({
        checkboxClass: 'icheckbox_square-red',
        cursor: true
    }).on('ifChanged', function (event) {
        $delBtn.prop('disabled', getCheckedBoxSize($checkbox).length < 1);
    });
}

// 获取选择的CheckBox[value]
function getCheckedBoxSize($checkbox){
    var checked = [];
    $checkbox.each(function(){
        if($(this).prop('checked')){
            checked.push($(this).val());
        }
    });
    return checked;
}

// 绑定表关系点击事件
function bindMappingTableGroupListClick() {
    var $tableGroupList = $("#tableGroupList");
    $tableGroupList.unbind("click");
    $tableGroupList.find("tr").bind('click', function () {
        doLoader('/tableGroup/page/editTableGroup?id=' + $(this).attr("id"));
    });

    // 绑定表格拖拽事件
    $tableGroupList.tableDnD({
        onDrop: function(table, row) {
            var newData = [];
            var $trList = $(table).find("tr");
            $.each($trList, function () {
                newData.push($(this).attr('id'));
            });
            $("#sortedTableGroupIds").val(newData.join('|'));
        }
    });
}

// 绑定下拉选择事件自动匹配相似表事件
function bindTableSelect(){
    const $sourceSelect = $("#sourceTable");
    const $targetSelect = $("#targetTable");
    $sourceSelect.on('changed.bs.select',function(e){
        $targetSelect.selectpicker('val', $(this).selectpicker('val'));
    });
    bindMappingTableGroupAddClick($sourceSelect, $targetSelect);
}

// 绑定下拉过滤按钮点击事件
function bindMultipleSelectFilterBtnClick() {
    $(".actions-btn").parent().append('<button type="button" class="actions-btn bs-show-all btn btn-default" title="显示所有表，包含已添加的表">取消过滤</button><button type="button" class="actions-btn bs-exclude-all btn btn-default" title="不显示已添加的表">过滤</button>');
    $(".actions-btn").css('width', '25%');
    $(".bs-show-all").bind("click", function () {
        doLoader('/mapping/page/edit?id=' + $("#mappingId").val() + '&exclude=1');
        bootGrowl("取消过滤成功!", "success");
    });
    $(".bs-exclude-all").bind("click", function () {
        refresh($("#mappingId").val());
        bootGrowl("过滤成功!", "success");
    });
}

// 绑定新增表关系点击事件
function bindMappingTableGroupAddClick($sourceSelect, $targetSelect) {
    let $addBtn = $("#tableGroupAddBtn");
    $addBtn.unbind("click");
    $addBtn.bind('click', function () {
        let m = {};
        m.mappingId = $(this).attr("mappingId");
        m.sourceTable = $sourceSelect.selectpicker('val');
        m.targetTable = $targetSelect.selectpicker('val');
        if (undefined == m.sourceTable) {
            bootGrowl("请选择数据源表", "danger");
            return;
        }
        if (undefined == m.targetTable) {
            bootGrowl("请选择目标源表", "danger");
            return;
        }

        let sLen = m.sourceTable.length;
        let tLen = m.targetTable.length;
        if (sLen < 1) {
            bootGrowl("请选择数据源表", "danger");
            return;
        }
        if (tLen < 1) {
            bootGrowl("请选择目标源表", "danger");
            return;
        }
        if (sLen != tLen) {
            bootGrowl("表映射关系数量不一致，请检查源表和目标表关系", "danger");
            return;
        }

        m.sourceTable = m.sourceTable.join('|');
        m.targetTable = m.targetTable.join('|');
        m.sourceTablePK = $("#sourceTablePK").val();
        m.targetTablePK = $("#targetTablePK").val();

        doPoster("/tableGroup/add", m, function (data) {
            if (data.success == true) {
                bootGrowl("新增映射关系成功!", "success");
                refresh(m.mappingId);
            } else {
                bootGrowl(data.resultValue, "danger");
                if (data.status == 400) {
                    refresh(m.mappingId);
                }
            }
        });
    });
}

// 绑定删除表关系点击事件
function bindMappingTableGroupDelClick() {
    $("#tableGroupDelBtn").click(function () {
        var ids = getCheckedBoxSize($(".tableGroupCheckbox"));
        if (ids.length > 0) {
            var $mappingId = $(this).attr("mappingId");
            doPoster("/tableGroup/remove", {"mappingId": $mappingId, "ids" : ids.join()}, function (data) {
                if (data.success == true) {
                    bootGrowl("删除映射关系成功!", "success");
                    refresh($mappingId);
                } else {
                    bootGrowl(data.resultValue, "danger");
                }
            });
        }
    });
}

// 修改驱动名称
function mappingModifyName(){
    var $name = $("#mappingModifyName");
    var tmp = $name.text();
    $name.text("");
    $name.append("<input type='text'/>");
    var $input = $name.find("input");
    $input.focus().val(tmp);
    $input.blur(function(){
        $name.text($(this).val());
        $("#mappingModifyForm input[name='name']").val($(this).val());
        $input.unbind();
    });
}

// 绑定刷新表事件
function bindRefreshTablesClick() {
    let $refreshBtn = $("#refreshTableBtn");
    $refreshBtn.bind('click', function(){
        let id = $(this).attr("tableGroupId");
        doPoster("/mapping/refreshTables", {'id': id}, function (data) {
            if (data.success == true) {
                bootGrowl("刷新表成功!", "success");
                refresh(id);
            } else {
                bootGrowl(data.resultValue, "danger");
            }
        });
    });
}

$(function () {
    // 绑定同步方式切换事件
    bindMappingModelChange();
    // 绑定删除表映射事件
    bindMappingTableGroupCheckBoxClick();

    // 绑定表关系点击事件
    bindMappingTableGroupListClick();
    // 绑定下拉选择事件自动匹配相似表事件
    bindTableSelect();
    // 绑定多值输入框事件
    initMultipleInputTags();
    // 绑定删除表关系点击事件
    bindMappingTableGroupDelClick();
    //绑定刷新数据表按钮点击事件
    bindRefreshTablesClick();

    // 初始化select插件
    initSelectIndex($(".select-control-table"), -1);
    // 绑定下拉过滤按钮点击事件
    bindMultipleSelectFilterBtnClick();

    // 保存
    $("#mappingSubmitBtn").click(function () {
        let $form = $("#mappingModifyForm");
        if ($form.formValidate() == true) {
            let data = $form.serializeJson();
            submit(data);
        }
    });

    // 返回
    $("#mappingBackBtn").click(function () {
        backIndexPage();
    });
})