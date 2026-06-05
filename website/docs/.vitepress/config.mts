import { defineConfig } from 'vitepress'

const socialLinks = [
  { icon: 'github', link: 'https://github.com/Hcrab/RTSbuilding' },
  { icon: 'discord', link: 'https://discord.gg/9Pw6vZfAm' }
]

const zhSidebar = {
  '/guide/': [
    {
      text: '入门',
      items: [
        { text: '操作指南总览', link: '/guide/' },
        { text: '如何下载', link: '/guide/download' },
        { text: '开始使用', link: '/guide/getting-started' },
        { text: '常见问题', link: '/guide/faq' }
      ]
    },
    {
      text: '常见操作',
      items: [
        { text: '远程建造', link: '/guide/common-actions/remote-building' },
        { text: '容器与 RTS 存储', link: '/guide/common-actions/container-storage' },
        { text: '仅提取绑定', link: '/guide/common-actions/extract-only-binding' }
      ]
    },
    {
      text: '顶部栏',
      items: [
        { text: '顶部栏总览', link: '/guide/topbar/overview' },
        { text: '模式和按钮', link: '/guide/topbar/modes-and-buttons' }
      ]
    },
    {
      text: '底部栏',
      items: [
        { text: '底部栏总览', link: '/guide/bottombar/overview' },
        { text: '左侧控制区', link: '/guide/bottombar/left-controls' },
        { text: '储存空间', link: '/guide/bottombar/storage' },
        { text: '创造物品栏', link: '/guide/bottombar/creative-picker' },
        { text: '蓝图空间', link: '/guide/bottombar/blueprints' },
        { text: '合成面板', link: '/guide/bottombar/crafting' }
      ]
    },
    {
      text: '侧边悬浮窗',
      items: [
        { text: '侧边悬浮窗', link: '/guide/floating-window/' }
      ]
    },
    {
      text: '生存模式机制',
      items: [
        { text: '技能树与能力解锁', link: '/guide/survival/skill-tree' },
        { text: 'RTS 家园与范围限制', link: '/guide/survival/home-range' }
      ]
    },
    {
      text: '设置与配置',
      items: [
        { text: '设置面板', link: '/guide/settings/panel' },
        { text: '整合包与服务器配置', link: '/guide/settings/server-config' }
      ]
    }
  ]
}

const enSidebar = {
  '/en/guide/': [
    {
      text: 'Getting Started',
      items: [
        { text: 'Guide Overview', link: '/en/guide/' },
        { text: 'Download', link: '/en/guide/download' },
        { text: 'First Steps', link: '/en/guide/getting-started' },
        { text: 'FAQ', link: '/en/guide/faq' }
      ]
    },
    {
      text: 'Common Actions',
      items: [
        { text: 'Remote Building', link: '/en/guide/common-actions/remote-building' },
        { text: 'Containers and RTS Storage', link: '/en/guide/common-actions/container-storage' },
        { text: 'Extract-Only Binding', link: '/en/guide/common-actions/extract-only-binding' }
      ]
    },
    {
      text: 'Top Bar',
      items: [
        { text: 'Top Bar Overview', link: '/en/guide/topbar/overview' },
        { text: 'Modes and Buttons', link: '/en/guide/topbar/modes-and-buttons' }
      ]
    },
    {
      text: 'Bottom Bar',
      items: [
        { text: 'Bottom Bar Overview', link: '/en/guide/bottombar/overview' },
        { text: 'Left Controls', link: '/en/guide/bottombar/left-controls' },
        { text: 'Storage Space', link: '/en/guide/bottombar/storage' },
        { text: 'Creative Item Picker', link: '/en/guide/bottombar/creative-picker' },
        { text: 'Blueprint Space', link: '/en/guide/bottombar/blueprints' },
        { text: 'Crafting Panel', link: '/en/guide/bottombar/crafting' }
      ]
    },
    {
      text: 'Side Floating Window',
      items: [
        { text: 'Side Floating Window', link: '/en/guide/floating-window/' }
      ]
    },
    {
      text: 'Survival Systems',
      items: [
        { text: 'Skill Tree and Unlocks', link: '/en/guide/survival/skill-tree' },
        { text: 'RTS Home and Range', link: '/en/guide/survival/home-range' }
      ]
    },
    {
      text: 'Settings',
      items: [
        { text: 'Settings Panel', link: '/en/guide/settings/panel' },
        { text: 'Modpack and Server Config', link: '/en/guide/settings/server-config' }
      ]
    }
  ]
}

export default defineConfig({
  lang: 'zh-CN',
  title: 'RTS Building',
  description: '从上帝视角建造 - Minecraft 模组文档',
  cleanUrls: false,
  head: [
    ['link', { rel: 'icon', href: '/images/logo.png' }]
  ],
  themeConfig: {
    search: {
      provider: 'local'
    }
  },
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'RTS Building',
      description: '从上帝视角建造 - Minecraft 模组文档',
      themeConfig: {
        logo: '/images/logo.png',
        nav: [
          { text: '首页', link: '/' },
          { text: '下载', link: '/guide/download' },
          { text: '文档', link: '/guide/' },
          { text: '常见问题', link: '/guide/faq' },
          { text: 'GitHub', link: 'https://github.com/Hcrab/RTSbuilding' }
        ],
        sidebar: zhSidebar,
        outline: {
          level: [2, 3],
          label: '页面目录'
        },
        langMenuLabel: '切换语言',
        returnToTopLabel: '返回顶部',
        search: {
          provider: 'local',
          options: {
            translations: {
              button: {
                buttonText: '搜索文档',
                buttonAriaLabel: '搜索文档'
              },
              modal: {
                noResultsText: '没有找到相关结果',
                resetButtonTitle: '清除查询条件',
                footer: {
                  selectText: '选择',
                  navigateText: '切换',
                  closeText: '关闭'
                }
              }
            }
          }
        },
        socialLinks,
        footer: {
          message: 'RTS Building - Build From Above',
          copyright: 'Licensed under LGPL-3.0'
        },
        docFooter: {
          prev: '上一页',
          next: '下一页'
        }
      }
    },
    en: {
      label: 'English',
      lang: 'en-US',
      title: 'RTS Building',
      description: 'Build from above - Minecraft mod documentation',
      link: '/en/',
      themeConfig: {
        logo: '/images/logo.png',
        nav: [
          { text: 'Home', link: '/en/' },
          { text: 'Download', link: '/en/guide/download' },
          { text: 'Docs', link: '/en/guide/' },
          { text: 'FAQ', link: '/en/guide/faq' },
          { text: 'GitHub', link: 'https://github.com/Hcrab/RTSbuilding' }
        ],
        sidebar: enSidebar,
        outline: {
          level: [2, 3],
          label: 'On This Page'
        },
        langMenuLabel: 'Change Language',
        returnToTopLabel: 'Return to Top',
        search: {
          provider: 'local'
        },
        socialLinks,
        footer: {
          message: 'RTS Building - Build From Above',
          copyright: 'Licensed under LGPL-3.0'
        },
        docFooter: {
          prev: 'Previous',
          next: 'Next'
        }
      }
    }
  }
})
