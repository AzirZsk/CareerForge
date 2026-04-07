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

// 注册图标到库
library.add(
  faBullseye,
  faFileLines,
  faComments,
  faStar,
  faFileAlt,
  faCircleCheck,
  faBriefcase,
  faFilePen,
  faPencil,
  faChartLine
)

// 导出组件供全局注册
export { FontAwesomeIcon }
