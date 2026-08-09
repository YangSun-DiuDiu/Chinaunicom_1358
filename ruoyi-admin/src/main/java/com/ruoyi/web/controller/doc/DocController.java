package com.ruoyi.web.controller.doc;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IDocService;

/**
 * 文档中心
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/doc")
public class DocController extends BaseController
{
    @Autowired
    private IDocService docService;

    @GetMapping("/tree")
    public AjaxResult tree()
    {
        List<Map<String, Object>> tree = docService.getTree();
        return success(tree);
    }

    @GetMapping("/content")
    public AjaxResult content(@RequestParam("path") String path)
    {
        Map<String, Object> content = docService.getContent(path);
        if (content.containsKey("error"))
            return error((String) content.get("error"));
        return success(content);
    }

    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(docService.getStatus());
    }

    @PreAuthorize("@ss.hasPermi('doc:sync')")
    @PostMapping("/sync")
    public AjaxResult sync()
    {
        return success(docService.syncAll());
    }

    @GetMapping("/search")
    public AjaxResult search(@RequestParam("keyword") String keyword)
    {
        return success(docService.search(keyword));
    }
}
