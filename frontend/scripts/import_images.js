#!/usr/bin/env node
/**
 * scripts/import_images.js
 *
 * 用途：将数据库导出的图片映射导入到前端 `src/data/images.js`。
 * 支持两种模式：
 *  1) 从 JSON 文件导入：JSON 应为数组，元素为 { attraction_id: number, url: string }
 *     示例：[{"attraction_id":11,"url":"https://.../img1.jpg"}, ...]
 *  2) 从目录扫描导入：按文件名约定 `attraction_<id>_*.jpg` 或 `<id>_*.jpg` 匹配归类。
 *
 * 输出：覆盖或生成 `src/data/images.js`（会备份旧文件为 `.bak`）。
 *
 * 用法示例：
 *  node scripts/import_images.js --mode=json --input=./export/images.json
 *  node scripts/import_images.js --mode=dir  --input=./exports/images_folder
 *
 */

const fs = require('fs')
const path = require('path')

function usage() {
  console.log('\nUsage: node scripts/import_images.js --mode=json|dir --input=PATH [--out=path] [--publicPrefix=/]')
  console.log('\nOptions:')
  console.log('  --mode=json|dir     导入模式：json（使用 JSON 映射）或 dir（扫描目录）')
  console.log('  --input=PATH        JSON 文件路径或目录路径')
  console.log('  --out=PATH          输出文件，默认 src/data/images.js')
  console.log('  --publicPrefix=URL  如果扫描目录，生成的路径前缀（例如 /public/images/ 或 /uploads/）')
  console.log('\nExamples:')
  console.log('  node scripts/import_images.js --mode=json --input=./images_export.json')
  console.log('  node scripts/import_images.js --mode=dir --input=./images --publicPrefix=/images/')
  process.exit(1)
}

// 解析参数
// ensure dependency minimist exists; simple fallback parser if not
let argv
try {
  argv = require('minimist')(process.argv.slice(2))
} catch (e) {
  // very small fallback parser
  argv = {}
  process.argv.slice(2).forEach(arg => {
    const m = arg.match(/^--([^=]+)=?(.*)$/)
    if (m) argv[m[1]] = m[2] || true
  })
}
const mode = argv.mode
const input = argv.input
const out = argv.out || path.join(__dirname, '..', 'src', 'data', 'images.js')
const publicPrefix = argv.publicPrefix || '/'

if (!mode || !input) usage()

async function run() {
  if (!['json', 'dir'].includes(mode)) {
    console.error('mode 必须是 json 或 dir')
    usage()
  }

  let mapping = {} // { id: [url,...] }

  if (mode === 'json') {
    const content = fs.readFileSync(input, 'utf-8')
    let arr
    try {
      arr = JSON.parse(content)
    } catch (e) {
      console.error('无法解析 JSON：', e.message)
      process.exit(2)
    }
    if (!Array.isArray(arr)) {
      console.error('JSON 必须为数组，元素格式：{ attraction_id, url }')
      process.exit(2)
    }
    arr.forEach(item => {
      const id = String(item.attraction_id || item.id || item.attractionId)
      const url = item.url || item.file_path || item.path
      if (!id || !url) return
      mapping[id] = mapping[id] || []
      mapping[id].push(url)
    })
  } else if (mode === 'dir') {
    const files = fs.readdirSync(input)
    files.forEach(fname => {
      const full = path.join(input, fname)
      const stat = fs.statSync(full)
      if (!stat.isFile()) return
      // 尝试从文件名中提取 id: 支持 attraction_11_1.jpg 或 11_1.jpg
      const m = fname.match(/attraction[_-]?(\d+)|^(\d+)[_-]/i)
      const id = m ? (m[1] || m[2]) : null
      const relPath = path.posix.join(publicPrefix.replace(/\\/g, '/'), fname)
      if (id) {
        mapping[id] = mapping[id] || []
        mapping[id].push(relPath)
      }
    })
  }

  // 准备输出内容
  const header = `// 自动生成 - images mapping\n// 生成时间: ${new Date().toISOString()}\n\n`
  const body = `const images = ${JSON.stringify(mapping, null, 2)}\n\nexport function getImagesForAttraction(id) {\n  return images[id] || []\n}\n\nexport default images\n`
  const contentOut = header + body

  // 备份
  try {
    if (fs.existsSync(out)) {
      const bak = out + '.bak'
      fs.copyFileSync(out, bak)
      console.log('已备份旧文件到', bak)
    }
  } catch (e) {
    console.warn('备份旧文件失败:', e.message)
  }

  // 写入
  fs.mkdirSync(path.dirname(out), { recursive: true })
  fs.writeFileSync(out, contentOut, 'utf-8')
  console.log('已生成', out)
}

run().catch(err => {
  console.error(err)
  process.exit(3)
})
