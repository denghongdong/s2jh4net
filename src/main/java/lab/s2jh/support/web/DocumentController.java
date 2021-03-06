package lab.s2jh.support.web;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.servlet.http.HttpServletRequest;

import lab.s2jh.core.annotation.MetaData;
import lab.s2jh.core.entity.BaseNativeEntity;
import lab.s2jh.core.service.BaseService;
import lab.s2jh.core.service.GlobalConfigService;
import lab.s2jh.core.util.DateUtils;
import lab.s2jh.core.web.BaseController;
import lab.s2jh.core.web.filter.WebAppContextInitFilter;
import lab.s2jh.core.web.view.OperationResult;
import lab.s2jh.module.auth.entity.Department;
import lab.s2jh.module.auth.service.DepartmentService;
import lab.s2jh.module.auth.service.UserService;
import lab.s2jh.module.sys.service.MenuService;
import lab.s2jh.support.web.DocumentController.MockEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.markdown4j.Markdown4jProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Controller
public class DocumentController extends BaseController<MockEntity, Long> {

    private final static Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private DepartmentService departmentService;

    Markdown4jProcessor markdown4jProcessor = new Markdown4jProcessor();

    @RequestMapping(value = "/docs/markdown/{name}", method = RequestMethod.GET)
    public String markdown(@PathVariable("name") String name, Model model) throws IOException {
        String mdDirPath = WebAppContextInitFilter.getInitedWebContextRealPath() + "/docs/markdown";
        File mdDir = new File(mdDirPath);
        String[] files = mdDir.list();
        for (int i = 0; i < files.length; i++) {
            files[i] = StringUtils.substringBeforeLast(files[i], ".md");
        }
        model.addAttribute("files", files);

        String mdFilePath = mdDirPath + "/" + name + ".md";
        model.addAttribute("mdHtml", markdown4jProcessor.process(FileUtils.readFileToString(new File(mdFilePath), "UTF-8")));
        return "layouts/markdown";
    }

    @Override
    protected BaseService<MockEntity, Long> getEntityService() {
        return null;
    }

    @Override
    protected MockEntity buildBindingEntity() {
        return new MockEntity();
    }

    @ModelAttribute
    public void prepareModel(HttpServletRequest request, Model model, @RequestParam(value = "id", required = false) Long id) {
        super.initPrepareModel(request, model, id);
    }

    @RequestMapping(value = "/admin/docs/ui-feature", method = RequestMethod.GET)
    public String uiFeature(Model model) {
        return "admin/docs/ui-feature";
    }

    @RequestMapping(value = "/docs/ui-feature/items", method = RequestMethod.GET)
    public String uiFeatureItems(Model model) {
        Map<Long, String> multiSelectItems = Maps.newLinkedHashMap();
        multiSelectItems.put(1L, "选项AAA");
        multiSelectItems.put(2L, "中文BBB");
        multiSelectItems.put(3L, "选项CCC");
        multiSelectItems.put(4L, "元素DDD");
        model.addAttribute("multiSelectItems", multiSelectItems);

        MockEntity entity = new MockEntity();
        entity.setSelectedIds(new Long[] { 2L });
        model.addAttribute("entity", entity);
        return "admin/docs/ui-feature-items";
    }

    @RequestMapping(value = "/docs/mock/tree-datas", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> mockTreeDatas(Model model) {
        List<Department> items = departmentService.findRoots();
        List<Map<String, Object>> treeDatas = Lists.newArrayList();

        for (Department item : items) {
            loopTreeData(treeDatas, item);
        }
        return treeDatas;
    }

    private void loopTreeData(List<Map<String, Object>> treeDatas, Department item) {
        Map<String, Object> row = Maps.newHashMap();
        treeDatas.add(row);
        row.put("id", item.getId());
        row.put("name", item.getDisplay());
        row.put("level", item.getLevel());
        row.put("open", false);
        List<Department> children = departmentService.findChildren(item);
        if (!CollectionUtils.isEmpty(children)) {
            List<Map<String, Object>> childrenList = Lists.newArrayList();
            row.put("children", childrenList);
            for (Department child : children) {
                loopTreeData(childrenList, child);
            }
        }
    }

    @RequestMapping(value = "/docs/mock/tags", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> tagsData(Model model, @RequestParam("q") String q) {
        List<Map<String, Object>> items = Lists.newArrayList();
        for (int i = 0, length = new Double((5 + Math.random() * 10)).intValue(); i < length; i++) {
            Map<String, Object> item = Maps.newHashMap();
            String txt = q + "模拟选项" + i;
            item.put("id", txt);
            item.put("text", txt);
            items.add(item);
        }
        return items;
    }

    @RequestMapping(value = "/docs/ui-feature/dropdownselect", method = RequestMethod.GET)
    public String uiFeatureDropdownselect(Model model) {
        return "admin/docs/ui-feature-dropdownselect";
    }

    public Map<Long, Object> mockRemoteSelectOptions(Model model, @RequestParam("code") String code) {
        Map<Long, Object> data = Maps.newLinkedHashMap();
        for (long i = 0; i < 10; i++) {
            data.put(i, code + "选项" + i);
        }
        return data;
    }

    @RequestMapping(value = "/docs/mock/dynamic-table", method = RequestMethod.POST)
    @ResponseBody
    public OperationResult saveDynamicTable(@ModelAttribute("entity") MockEntity entity, Model model) {
        logger.debug("MockEntity: {}", entity);

        //处理关联对象删除
        List<MockItemEntity> items = entity.getMockItemEntites();
        if (CollectionUtils.isNotEmpty(items)) {
            List<MockItemEntity> toRemoves = Lists.newArrayList();
            for (MockItemEntity item : items) {
                logger.debug("MockItemEntity: {}", item);
                if (item.isMarkedRemove()) {
                    toRemoves.add(item);
                }
            }
            items.removeAll(toRemoves);
        }

        return OperationResult.buildSuccessResult("数据处理成功");
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @Access(AccessType.FIELD)
    public static class MockEntity extends BaseNativeEntity {
        private static final long serialVersionUID = -9187040717483523936L;

        private Department department;

        private String filePath;

        private Long selectedId;

        private Long[] selectedIds;

        @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_FORMAT)
        private Date saleDate = new Date();

        @DateTimeFormat(pattern = DateUtils.SHORT_TIME_FORMAT)
        private Date publishTime = new Date();

        private Date searchDate;

        private String textContent;

        private String htmlContent;

        private String splitText;

        @MetaData(value = "图片路径数组", comments = "用于UI表单数据收集，实际可根据设计转换为另外的逗号分隔字符串存储属性")
        private String[] imagePaths;

        private BigDecimal quantity;

        private Boolean expired;

        private List<MockItemEntity> mockItemEntites;

        @Override
        public String toString() {
            return "MockEntity [department=" + department + ", filePath=" + filePath + ", selectedId=" + selectedId + ", selectedIds="
                    + Arrays.toString(selectedIds) + ", saleDate=" + saleDate + ", publishTime=" + publishTime + ", searchDate=" + searchDate
                    + ", textContent=" + textContent + ", htmlContent=" + htmlContent + ", splitText=" + splitText + ", imagePaths="
                    + Arrays.toString(imagePaths) + ", quantity=" + quantity + "]";
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @Access(AccessType.FIELD)
    public static class MockItemEntity extends BaseNativeEntity {

        private static final long serialVersionUID = -2048009679981043819L;

        private MockEntity mockEntity;

        private Department department;

        @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_FORMAT)
        private Date saleDate = new Date();

        private String textContent;

        private String imagePath;

        @Override
        public String toString() {
            return "MockItemEntity [department=" + department + ", saleDate=" + saleDate + ", textContent=" + textContent + ", imagePath="
                    + imagePath + "]";
        }
    }

}
