/**
 * Font Awesome 图标库配置
 * @author Azir
 */
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

// 导入工作台页面需要的图标
import {
  faBullseye, // 开始面试
  faFileLines, // 优化简历
  faComments, // 面试统计
  faStar, // 模拟面试
  faFileAlt, // 简历
  faCircleCheck, // 完成率
  faBriefcase, // 工作动态
  faFilePen, // 简历动态
  faPencil, // 练习动态
  faChartLine // 统计动态
} from '@fortawesome/free-solid-svg-icons'

// 导入简历模块需要的图标 - 操作类
import {
  faXmark, // 关闭
  faPlus, // 添加
  faPenToSquare, // 编辑
  faArrowUpRightFromSquare, // 外部链接
  faLocationDot, // 位置
  faTrash // 删除
} from '@fortawesome/free-solid-svg-icons'

// 导入简历模块需要的图标 - 状态类
import {
  faCheck, // 完成
  faCircleXmark, // 失败
  faSpinner, // 加载中
  faArrowsRotate // 刷新/重试
} from '@fortawesome/free-solid-svg-icons'

// 导入简历模块需要的图标 - 导航/功能类
import {
  faChevronDown, // 下拉箭头
  faMagnifyingGlass, // 搜索
  faTableCells, // 网格/结构
  faCircleInfo // 信息
} from '@fortawesome/free-solid-svg-icons'

// 导入简历模块需要的图标 - 模块类型
import {
  faUser, // 基本信息
  faGraduationCap, // 教育经历
  faDiagramProject, // 项目经历
  faBolt, // 专业技能
  faTrophy, // 证书荣誉
  faCodeBranch, // 开源贡献
  faRectangleList // 自定义区块
} from '@fortawesome/free-solid-svg-icons'

// 导入简历模块需要的图标 - 建议类型
import {
  faTriangleExclamation, // 严重问题
  faLightbulb, // 改进建议
  faWandMagicSparkles // 优化建议
} from '@fortawesome/free-solid-svg-icons'

// 导入面试中心模块需要的图标 - 操作/功能类
import {
  faArrowLeft, // 返回
  faLink, // 链接
  faCalendar, // 日历
  faMicrophone, // 麦克风
  faMicrophoneSlash, // 麦克风禁用
  faGear, // 设置
  faKey, // 密码
  faPaperclip, // 附件
  faVideo, // 视频
  faLaptop, // 电脑
  faStopwatch // 倒计时
} from '@fortawesome/free-solid-svg-icons'

// 导入面试中心模块需要的图标 - 内容/类型类
import {
  faRobot, // AI 机器人
  faClipboard, // 剪贴板
  faClipboardList, // 剪贴板列表
  faBuilding, // 公司
  faPen, // 笔/编辑
  faStickyNote, // 便签
  faFire, // 火
  faMugHot, // 咖啡杯
  faThumbtack, // 图钉
  faCircleExclamation, // 错误提示
  faCircleQuestion // 快捷求助按钮
} from '@fortawesome/free-solid-svg-icons'

// 导入面试中心模块需要的图标 - 状态/指示类
import {
  faHourglassHalf, // 等待
  faChartBar, // 统计图表
  faChevronRight, // 右箭头
  faEllipsisVertical // 更多操作（竖向省略号）
} from '@fortawesome/free-solid-svg-icons'

// 导入通知模块需要的图标
import {
  faBell, // 通知铃铛
  faInbox, // 空收件箱
  faRotate // 旋转/进行中
} from '@fortawesome/free-solid-svg-icons'

// 导入 AI Chat 模块需要的图标
import {
  faScrewdriverWrench, // 调整
  faPenFancy, // 创建
  faComment, // 通用聊天
  faPencilSquare // 默认
} from '@fortawesome/free-solid-svg-icons'

// 导入 emoji 替换需要的额外图标
import {
  faHand, // 挥手
  faFlask, // 实验/技术
  faBookOpen, // 书本/概念
  faBackwardStep, // 上一曲
  faForwardStep, // 下一曲
  faPlay, // 播放
  faPause, // 暂停
  faCircle, // 圆点/优先级
  faUserTie, // 正式用户/面试官
  faPerson, // 男性
  faPersonDress, // 女性
  faLock, // 锁/安全
  faDownload, // 下载
  faPaperPlane, // 发送/投递
  faChampagneGlasses, // 庆祝/Offer
  faArrowRotateLeft, // 撤回
  faBookmark // 书签/收藏
} from '@fortawesome/free-solid-svg-icons'

// 注册图标到库
library.add(
  // 工作台页面图标
  faBullseye,
  faFileLines,
  faComments,
  faStar,
  faFileAlt,
  faCircleCheck,
  faBriefcase,
  faFilePen,
  faPencil,
  faChartLine,
  // 简历模块 - 操作类
  faXmark,
  faPlus,
  faPenToSquare,
  faArrowUpRightFromSquare,
  faLocationDot,
  faTrash,
  // 简历模块 - 状态类
  faCheck,
  faCircleXmark,
  faSpinner,
  faArrowsRotate,
  // 简历模块 - 导航/功能类
  faChevronDown,
  faMagnifyingGlass,
  faTableCells,
  faCircleInfo,
  // 简历模块 - 模块类型
  faUser,
  faGraduationCap,
  faDiagramProject,
  faBolt,
  faTrophy,
  faCodeBranch,
  faRectangleList,
  // 简历模块 - 建议类型
  faTriangleExclamation,
  faLightbulb,
  faWandMagicSparkles,
  // 面试中心 - 操作/功能类
  faArrowLeft,
  faLink,
  faCalendar,
  faMicrophone,
  faMicrophoneSlash,
  faGear,
  faKey,
  faPaperclip,
  faVideo,
  faLaptop,
  faStopwatch,
  // 面试中心 - 内容/类型类
  faRobot,
  faClipboard,
  faClipboardList,
  faBuilding,
  faPen,
  faStickyNote,
  faFire,
  faMugHot,
  faThumbtack,
  faCircleExclamation,
  faCircleQuestion,
  // 面试中心 - 状态/指示类
  faHourglassHalf,
  faChartBar,
  faChevronRight,
  faEllipsisVertical,
  // 通知模块
  faBell,
  faInbox,
  faRotate,
  // AI Chat 模块
  faScrewdriverWrench,
  faPenFancy,
  faComment,
  faPencilSquare,
  // Emoji 替换图标
  faHand,
  faFlask,
  faBookOpen,
  faBackwardStep,
  faForwardStep,
  faPlay,
  faPause,
  faCircle,
  faUserTie,
  faPerson,
  faPersonDress,
  faLock,
  faDownload,
  faPaperPlane,
  faChampagneGlasses,
  faArrowRotateLeft,
  faBookmark
)

// 导出组件供全局注册
export { FontAwesomeIcon }
