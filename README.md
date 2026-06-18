# 🚀 火箭燃料推进系统参数仿真平台

基于 Java Spring Boot + Vue 3 构建的火箭燃料推进系统参数仿真工具，支持液氧/煤油（LOX/RP-1）推进剂配比仿真计算。

## ✨ 功能特性

- 🔧 **参数配置**：自定义燃料配比、燃烧室压力、初始温度等核心参数
- 🧪 **物理仿真**：基于真实火箭推进物理模型的核心计算引擎
- 📊 **可视化展示**：推力曲线、排气速度、比冲等关键性能参数
- 💾 **配方管理**：保存、编辑、删除仿真配方，支持快速复用
- 🎨 **现代UI**：响应式设计，美观的渐变配色和流畅动画
- 📈 **图表交互**：支持折线图/柱状图/面积图切换，数据缩放与导出

## 🛠️ 技术栈

### 后端
- **Java 17** + **Spring Boot 3.2.x**
- **Spring Data JPA** + **PostgreSQL 16**
- **Hibernate Validator** 参数校验
- **Lombok** 简化代码
- **CORS** 跨域支持

### 前端
- **Vue 3** (Composition API)
- **Vite 5** 构建工具
- **Element Plus** UI组件库
- **Pinia** 状态管理
- **ECharts 5** 数据可视化
- **Axios** HTTP客户端
- **Vue Router 4** 路由管理

## 📁 项目结构

```
cn9/
├── backend/                          # 后端 Spring Boot 项目
│   ├── src/
│   │   └── main/
│   │       ├── java/com/rocket/simulation/
│   │       │   ├── RocketFuelSimulationApplication.java   # 启动类
│   │       │   ├── config/                                 # 配置类
│   │       │   │   └── CorsConfig.java
│   │       │   ├── controller/                             # REST API 控制器
│   │       │   │   └── SimulationController.java
│   │       │   ├── dto/                                    # 数据传输对象
│   │       │   │   ├── SimulationRequest.java
│   │       │   │   └── SimulationResult.java
│   │       │   ├── entity/                                 # JPA 实体
│   │       │   │   └── SimulationRecipe.java
│   │       │   ├── exception/                              # 异常处理
│   │       │   │   ├── GlobalExceptionHandler.java
│   │       │   │   └── ResourceNotFoundException.java
│   │       │   ├── repository/                             # 数据访问层
│   │       │   │   └── SimulationRecipeRepository.java
│   │       │   └── service/                                # 业务逻辑层
│   │       │       ├── PropulsionCalculationService.java   # 核心计算引擎
│   │       │       └── SimulationService.java              # 业务服务
│   │       └── resources/
│   │           ├── application.yml                          # 应用配置
│   │           └── db/schema.sql                            # 数据库脚本
│   └── pom.xml                                             # Maven 配置
│
├── frontend/                         # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/simulation.js         # API 接口
│   │   ├── components/
│   │   │   └── ThrustChart.vue       # 推力曲线图组件
│   │   ├── router/index.js           # 路由配置
│   │   ├── stores/simulation.js      # Pinia 状态管理
│   │   ├── styles/global.scss        # 全局样式
│   │   ├── views/
│   │   │   ├── Simulation.vue        # 仿真计算页面
│   │   │   └── Recipes.vue           # 配方管理页面
│   │   ├── App.vue                   # 根组件
│   │   └── main.js                   # 入口文件
│   ├── public/rocket.svg             # Favicon
│   ├── index.html                    # HTML 入口
│   ├── vite.config.js                # Vite 配置
│   └── package.json                  # NPM 依赖
│
├── docker-compose.yml                # Docker Compose 配置
└── README.md
```

## 🧮 核心数学模型

### 1. 燃烧热力学计算
基于氧燃比（O/F）计算燃烧产物的温度、平均摩尔质量和比热比：

```
T_flame = f(O/F, T_initial, P_chamber)
M_mix = f(O/F)
γ = f(O/F)
```

### 2. 特征速度 C*
```
C* = √(γ·R·Tc) / [γ·√(2/(γ+1)) · (2/(γ+1))^((γ+1)/(2(γ-1)))]
```

### 3. 喷管流动计算
使用牛顿迭代法求解出口马赫数，基于面积比公式：

```
Ae/At = (1/Me) · [2/(γ+1) · (1 + (γ-1)/2·Me²)]^((γ+1)/(2(γ-1)))
```

### 4. 排气速度
```
Ve = Me · √(γ·R·Te)
```

### 5. 推力计算
```
F = ṁ·Ve + (Pe - Pa)·Ae
```

### 6. 比冲
```
Isp = Ve / g0
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- PostgreSQL 14+ （或 Docker）
- Maven 3.8+

### 方式一：使用 Docker 快速部署（推荐）

```bash
# 1. 启动 PostgreSQL 数据库
docker-compose up -d

# 2. 编译并启动后端
cd backend
mvn clean package -DskipTests
java -jar target/rocket-fuel-simulation-1.0.0.jar

# 3. 启动前端（新终端）
cd frontend
npm install
npm run dev
```

### 方式二：手动部署

#### 1. 数据库配置
```sql
-- 创建数据库
CREATE DATABASE rocket_simulation;

-- 执行初始化脚本
-- backend/src/main/resources/db/schema.sql
```

#### 2. 修改后端配置
编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rocket_simulation
    username: your_username
    password: your_password
```

#### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

#### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```

前端开发服务将在 `http://localhost:5173` 启动

## 📡 API 接口文档

### 配方管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/simulation/recipes` | 获取所有配方（支持 keyword 查询参数） |
| GET | `/api/simulation/recipes/{id}` | 根据ID获取配方 |
| POST | `/api/simulation/recipes` | 创建新配方 |
| PUT | `/api/simulation/recipes/{id}` | 更新配方 |
| DELETE | `/api/simulation/recipes/{id}` | 删除配方 |

### 仿真计算

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/simulation/run` | 运行仿真并保存配方 |
| POST | `/api/simulation/run/{id}` | 使用已有配方ID运行仿真 |
| POST | `/api/simulation/preview` | 预览仿真结果（不保存） |

### 请求参数示例

```json
{
  "name": "标准液氧煤油配方",
  "description": "用于中型运载火箭的标准推进剂配方",
  "loxRatio": 2.56,
  "keroseneRatio": 1.0,
  "chamberPressure": 10.0,
  "initialTemperature": 300.0,
  "burnDuration": 120.0,
  "nozzleAreaRatio": 16.0
}
```

### 响应参数示例

```json
{
  "recipeId": 1,
  "recipeName": "标准液氧煤油配方",
  "exhaustVelocity": 2987.65,
  "specificImpulse": 304.67,
  "maxThrust": 1256.32,
  "averageThrust": 1189.45,
  "totalImpulse": 142734.0,
  "massFlowRate": 0.3987,
  "characteristicVelocity": 1765.43,
  "thrustCoefficient": 1.6928,
  "exitMachNumber": 3.24,
  "exitTemperature": 876.54,
  "exitPressure": 0.0876,
  "thrustCurve": [
    { "time": 0.0, "thrust": 0.0 },
    { "time": 0.1, "thrust": 342.5 },
    ...
  ]
}
```

## 💡 使用指南

### 1. 仿真计算
1. 访问 `http://localhost:5173`
2. 在「仿真参数配置」区域输入参数：
   - **配方名称**：为本次仿真命名以便保存
   - **燃料配比**：设置液氧（LOX）和煤油（RP-1）的质量比
   - **燃烧室参数**：设置燃烧室压力（MPa）和初始温度（K）
   - **燃烧与喷管**：设置燃烧时长和喷管面积比
3. 点击「开始仿真并保存」运行计算并保存配方
4. 或点击「仅预览结果」查看计算结果但不保存

### 2. 查看结果
- **顶部指标卡片**：最大推力、平均推力、排气速度、比冲
- **推力曲线图**：实时展示推力随时间变化曲线
  - 支持切换折线图/柱状图/面积图
  - 支持平滑/原始曲线切换
  - 鼠标滚轮缩放、拖动平移
  - 可导出为PNG图片
- **详细参数**：总冲量、质量流率、特征速度等完整参数

### 3. 配方管理
1. 切换到「配方管理」页面
2. 查看所有已保存的仿真配方
3. 支持按名称搜索配方
4. 操作按钮：
   - **运行**：直接使用该配方运行仿真
   - **加载**：将配方参数加载到仿真页面进行编辑
   - **删除**：删除指定配方

## 🔧 参数说明

| 参数 | 单位 | 范围 | 说明 |
|------|------|------|------|
| 液氧比例 (LOX) | 质量比 | 0.1 - 10.0 | 液氧在推进剂中的比例 |
| 煤油比例 (RP-1) | 质量比 | 0.1 - 10.0 | 煤油在推进剂中的比例 |
| 氧燃比 (O/F) | - | 自动计算 | 液氧/煤油质量比，化学当量比约为3.46 |
| 燃烧室压力 | MPa | 1.0 - 50.0 | 燃烧室内的压强 |
| 初始温度 | K | 200.0 - 800.0 | 推进剂进入燃烧室的初始温度 |
| 燃烧时长 | 秒 | 1.0 - 600.0 | 发动机持续工作时间 |
| 喷管面积比 | - | 1.0 - 100.0 | 喷管出口面积与喉部面积之比 |

## 🧪 默认示例配方

系统预置3个示例配方：
1. **标准液氧煤油配方** - O/F = 2.56，Pc = 10 MPa，t = 120s
2. **高空优化配方** - O/F = 2.8，Pc = 12 MPa，ε = 25（大喷管）
3. **大推力配方** - O/F = 2.4，Pc = 15 MPa，t = 90s

## 📝 开发说明

### 核心计算服务扩展
如需修改或扩展物理模型，请编辑：
[PropulsionCalculationService.java](backend/src/main/java/com/rocket/simulation/service/PropulsionCalculationService.java)

主要方法：
- `calculateCombustion()` - 燃烧热力学计算
- `calculateCharacteristicVelocity()` - 特征速度计算
- `calculateNozzleFlow()` - 喷管流动计算
- `findMachNumber()` - 马赫数迭代求解
- `generateThrustCurve()` - 推力曲线生成

### 添加新的推进剂类型
1. 在 `PropulsionCalculationService` 中添加新的燃料属性
2. 修改 `calculateCombustion()` 方法支持新的燃烧模型
3. 在前端添加对应的配方选项

## 🐛 常见问题

**Q: 后端启动失败，提示数据库连接错误**
A: 请确认 PostgreSQL 服务已启动，检查 `application.yml` 中的数据库配置。

**Q: 前端访问后端API跨域**
A: 项目已配置 CORS 和 Vite 代理，开发环境下通过 `/api` 前缀访问即可。

**Q: 计算结果不收敛**
A: 请检查输入参数是否在合理范围内，特别是喷管面积比不要过大或过小。

**Q: 如何修改计算精度？**
A: 在 `PropulsionCalculationService.findMachNumber()` 方法中调整 `tolerance` 和 `maxIterations` 参数。

## 📄 许可证

MIT License

## 👨‍💻 技术支持

如有问题或建议，欢迎提交 Issue。

---

**注意**：本仿真工具基于简化的物理模型，计算结果仅供参考。实际火箭发动机设计需要更复杂的分析和试验验证。
