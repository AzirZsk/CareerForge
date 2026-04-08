/**
 * Font Awesome 图标库配置
 * @author Azir
 */
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

// 导入工作台页面需要的图标
import {
  faBullseye, // 🎯 开始面试
  faFileLines, // 📄 优化简历
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
  faWandMagicSparkles
)

// 导出组件供全局注册
export { FontAwesomeIcon }
