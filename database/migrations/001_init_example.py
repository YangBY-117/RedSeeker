#这是实例sqlite3数据库创建文档代码，供格式参考：
#实际代码量3200行+，数据库存储了200+条景点与7段历史阶段（即历史事件表）
import sqlite3
import json
from enum import Enum
from typing import List, Dict, Any

# 景点类型枚举
class AttractionCategory(Enum):
    纪念馆 = 1
    烈士陵园 = 2
    会议旧址 = 3
    战役遗址 = 4
    名人故居 = 5
    革命根据地 = 6
    纪念碑塔 = 7
    博物馆 = 8
    其他纪念地 = 9

# 初始化SQLite数据库
def init_database(db_path: str = "red_tourism.db"):
    """
    初始化红色景点数据库
    """
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 创建景点表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS attractions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name VARCHAR(200) NOT NULL,
            address VARCHAR(300),
            longitude DECIMAL(10, 7),
            latitude DECIMAL(10, 7),
            category INTEGER NOT NULL,
            brief_intro TEXT,
            historical_background TEXT,
            per_capita_consumption INTEGER DEFAULT 0,
            business_hours TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # 创建历史事件表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS historical_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_name VARCHAR(200) NOT NULL,
            start_year INTEGER,
            end_year INTEGER,
            description TEXT,
            period VARCHAR(50)
        )
    ''')
    
    # 创建景点-事件关联表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS attraction_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            attraction_id INTEGER,
            event_id INTEGER,
            FOREIGN KEY (attraction_id) REFERENCES attractions(id),
            FOREIGN KEY (event_id) REFERENCES historical_events(id)
        )
    ''')
    
    conn.commit()
    conn.close()
    print(f"数据库初始化成功: {db_path}")

# 添加历史事件数据
def insert_historical_events(db_path: str = "red_tourism.db"):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    events = [
        (1, "中国共产党的创建", 1921, 1927, "中共一大召开，中国共产党成立", "建党初期"),
        (2, "南昌起义与土地革命", 1927, 1937, "八一南昌起义，创建人民军队，开辟农村革命根据地", "土地革命战争时期"),
        (3, "红军长征", 1934, 1936, "战略转移，保存革命火种", "土地革命战争时期"),
        (4, "抗日战争", 1937, 1945, "全民族抗战，打败日本侵略者", "抗日战争时期"),
        (5, "解放战争", 1945, 1949, "推翻国民党统治，建立新中国", "解放战争时期"),
        (6, "建国后建设", 1949, 1978, "社会主义建设时期", "建国后时期"),
        (7, "改革开放", 1978, None, "改革开放，现代化建设", "当代时期")
    ]
    
    cursor.executemany('''
        INSERT OR REPLACE INTO historical_events (id, event_name, start_year, end_year, description, period)
        VALUES (?, ?, ?, ?, ?, ?)
    ''', events)
    
    conn.commit()
    conn.close()
    print("历史事件数据插入成功")

# 添加景点数据
def insert_attractions(db_path: str = "red_tourism.db"):
    """
    按照历史事件脉络插入红色景点数据
    """
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 按照历史事件脉络组织的景点数据
    attractions: List[Dict[str, Any]] = [
        # 1. 建党初期 (1921-1927)
        {
            "name": "中共一大纪念馆",
            "address": "上海市黄浦区兴业路76号",
            "longitude": 121.4737,
            "latitude": 31.2208,
            "category": AttractionCategory.会议旧址.value,
            "brief_intro": "中国共产党第一次全国代表大会会址，中国革命的起源地",
            "historical_background": "1921年7月，中国共产党第一次全国代表大会在此召开，宣告中国共产党正式成立。这是开天辟地的大事变，中国革命面貌从此焕然一新。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "南湖革命纪念馆",
            "address": "浙江省嘉兴市南湖区烟雨路1号",
            "longitude": 120.7575,
            "latitude": 30.7536,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念中共一大在南湖红船上闭幕的历史事件",
            "historical_background": "1921年7月，中共一大因法租界巡捕袭扰，从上海转移到嘉兴南湖的一艘画舫上继续举行并胜利闭幕，庄严宣告了中国共产党的诞生。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:00（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "中共二大会址纪念馆",
            "address": "上海市静安区老成都北路7弄30号",
            "longitude": 121.4678,
            "latitude": 31.2304,
            "category": AttractionCategory.会议旧址.value,
            "brief_intro": "中国共产党第二次全国代表大会会址",
            "historical_background": "1922年7月，中共二大在此召开，首次提出反帝反封建的民主革命纲领，制定了中国共产党第一部党章。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "广州农民运动讲习所旧址",
            "address": "广东省广州市越秀区中山四路42号",
            "longitude": 113.2644,
            "latitude": 23.1291,
            "category": AttractionCategory.会议旧址.value,
            "brief_intro": "培养农民运动干部的重要场所",
            "historical_background": "1924-1926年，毛泽东等共产党人在这里主办了六届农民运动讲习所，培养了大批农民运动骨干，为中国革命奠定了重要的群众基础。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "安源路矿工人运动纪念馆",
            "address": "江西省萍乡市安源区安源镇",
            "longitude": 113.9500,
            "latitude": 27.6167,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "中国共产党领导工人运动的最早实践地之一",
            "historical_background": "1922年，毛泽东、李立三、刘少奇等在此领导了安源路矿工人大罢工，这是中国共产党第一次独立领导并取得完全胜利的工人斗争。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "京汉铁路总工会旧址",
            "address": "河南省郑州市二七区钱塘路",
            "longitude": 113.6253,
            "latitude": 34.7466,
            "category": AttractionCategory.会议旧址.value,
            "brief_intro": "京汉铁路工人大罢工指挥中心",
            "historical_background": "1923年2月，京汉铁路工人在此成立总工会，举行震惊中外的大罢工，虽遭血腥镇压，但显示了中国工人阶级的力量。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 1
        },
        {
            "name": "上海四行仓库抗战纪念馆",
            "address": "上海市静安区光复路1号",
            "longitude": 121.4751,
            "latitude": 31.2401,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "淞沪会战中八百壮士英勇抗战的遗址",
            "historical_background": "1937年淞沪会战期间，谢晋元率八百壮士在四行仓库孤军奋战四天四夜，粉碎了日军三个月灭亡中国的狂妄计划，极大鼓舞了全国军民的抗战决心。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 4
        },
        {
            "name": "南京大屠杀纪念馆",
            "address": "江苏省南京市建邺区水西门大街418号",
            "longitude": 118.7506,
            "latitude": 32.0422,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念南京大屠杀惨案，铭记历史珍爱和平",
            "historical_background": "1937年12月，侵华日军在南京制造了惨绝人寰的大屠杀，30万同胞惨遭杀害。纪念馆通过大量史实揭露日军暴行，警示后人勿忘国耻。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-16:30（周一闭馆）",
            "event_id": 4
        },
        {
            "name": "台儿庄大战纪念馆",
            "address": "山东省枣庄市台儿庄区沿河南路",
            "longitude": 117.7350,
            "latitude": 34.5576,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念抗战初期台儿庄大捷",
            "historical_background": "1938年春的台儿庄战役是自抗战以来中国军队的重大胜利，歼敌万余人，极大鼓舞了全国军民的抗战信心。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:00",
            "event_id": 4
        },
        {
            "name": "八路军太行纪念馆",
            "address": "山西省长治市武乡县太行街",
            "longitude": 112.8361,
            "latitude": 36.8333,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "全面反映八路军抗战历史的大型纪念馆",
            "historical_background": "抗日战争时期，八路军在太行山区创建抗日根据地，开展了著名的百团大战等战役，为抗战胜利作出巨大贡献。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 4
        },
        {
            "name": "新四军军部旧址",
            "address": "安徽省宣城市泾县云岭乡",
            "longitude": 118.4110,
            "latitude": 30.6914,
            "category": AttractionCategory.会议旧址.value,
            "brief_intro": "新四军军部驻地旧址",
            "historical_background": "1938-1941年，新四军军部驻此，指挥华中抗日游击战争。1941年1月，皖南事变爆发，新四军遭受重大损失。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:00",
            "event_id": 4
        },
        {
            "name": "皖南事变烈士陵园",
            "address": "安徽省宣城市泾县水西山路",
            "longitude": 118.4039,
            "latitude": 30.6844,
            "category": AttractionCategory.烈士陵园.value,
            "brief_intro": "纪念皖南事变中牺牲的新四军烈士",
            "historical_background": "1941年1月，皖南新四军9000余人在泾县茂林地区被国民党军8万余人包围，血战七昼夜，3000余人突出重围，4000余人被俘或失散，3000余人壮烈牺牲。",
            "per_capita_consumption": 0,
            "business_hours": "全天开放",
            "event_id": 4
        },
        
        # 5. 解放战争时期 (1945-1949)
        {
            "name": "辽沈战役纪念馆",
            "address": "辽宁省锦州市凌河区北京路五段1号",
            "longitude": 121.1400,
            "latitude": 41.1100,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "展示辽沈战役全过程的专题纪念馆",
            "historical_background": "1948年9-11月，东北野战军发起辽沈战役，歼灭国民党军47万余人，解放东北全境，为解放全中国奠定基础。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 5
        },
        {
            "name": "淮海战役纪念馆",
            "address": "江苏省徐州市泉山区解放南路2号",
            "longitude": 117.1848,
            "latitude": 34.2221,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念淮海战役胜利的专题纪念馆",
            "historical_background": "1948年11月-1949年1月，华东、中原野战军在以徐州为中心的广大地区发起淮海战役，歼灭国民党军55万余人，为渡江作战创造了条件。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 5
        },
        {
            "name": "平津战役纪念馆",
            "address": "天津市红桥区平津道8号",
            "longitude": 117.1346,
            "latitude": 39.1518,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念平津战役胜利的专题纪念馆",
            "historical_background": "1948年11月-1949年1月，东北野战军和华北军区部队发起平津战役，歼灭和改编国民党军52万余人，基本解放华北全境。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 5
        },
        {
            "name": "渡江战役纪念馆",
            "address": "安徽省合肥市包河区云谷路299号",
            "longitude": 117.3500,
            "latitude": 31.7333,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念百万雄师过大江的渡江战役",
            "historical_background": "1949年4月20日-6月2日，人民解放军百万大军发起渡江战役，突破长江天险，解放南京、上海等大城市，宣告国民党反动统治的覆灭。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00（周一闭馆）",
            "event_id": 5
        },
        {
            "name": "西柏坡纪念馆",
            "address": "河北省石家庄市平山县西柏坡镇",
            "longitude": 114.0294,
            "latitude": 38.3589,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "中共中央最后一个农村指挥所",
            "historical_background": "1948年5月-1949年3月，中共中央移驻西柏坡，指挥了三大战役，召开了七届二中全会，描绘了新中国的蓝图。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:30",
            "event_id": 5
        },
        {
            "name": "香山革命纪念馆",
            "address": "北京市海淀区买卖街40号",
            "longitude": 116.1934,
            "latitude": 39.9911,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "中共中央'进京赶考'第一站",
            "historical_background": "1949年3月25日-9月21日，中共中央从西柏坡迁至香山，指挥了渡江战役，筹备了新政协，为新中国的成立作了充分准备。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:00（周一闭馆）",
            "event_id": 5
        },
        {
            "name": "雨花台烈士陵园",
            "address": "江苏省南京市雨花台区雨花路215号",
            "longitude": 118.7628,
            "latitude": 31.9922,
            "category": AttractionCategory.烈士陵园.value,
            "brief_intro": "纪念新民主主义革命时期牺牲的雨花台烈士",
            "historical_background": "1927-1949年，近10万共产党人和革命志士在雨花台英勇就义。1950年建园，是全国规模最大的纪念性陵园之一。",
            "per_capita_consumption": 0,
            "business_hours": "08:00-17:00",
            "event_id": 5
        },
        {
            "name": "重庆歌乐山烈士陵园",
            "address": "重庆市沙坪坝区烈士墓政法村",
            "longitude": 106.4064,
            "latitude": 29.5633,
            "category": AttractionCategory.烈士陵园.value,
            "brief_intro": "纪念被国民党杀害的红岩英烈",
            "historical_background": "1949年11月27日，国民党特务在溃逃前夕，对关押在白公馆、渣滓洞的革命者进行大屠杀，300余人壮烈牺牲。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-17:00",
            "event_id": 5
        },
        {
            "name": "双清别墅",
            "address": "北京市海淀区香山公园内",
            "longitude": 116.1934,
            "latitude": 39.9911,
            "category": AttractionCategory.名人故居.value,
            "brief_intro": "毛泽东在香山的居住地",
            "historical_background": "1949年3月25日，毛泽东等中央领导人进驻香山，双清别墅成为毛泽东同志的居住地，是筹建新中国的历史见证地。",
            "per_capita_consumption": 15,  # 香山公园门票15元
            "business_hours": "06:00-18:00",
            "event_id": 5
        },
        {
            "name": "天安门广场",
            "address": "北京市东城区天安门广场",
            "longitude": 116.3975,
            "latitude": 39.9087,
            "category": AttractionCategory.其他纪念地.value,
            "brief_intro": "中华人民共和国成立的历史见证地",
            "historical_background": "1949年10月1日，毛泽东主席在天安门城楼庄严宣告中华人民共和国成立，标志着新民主主义革命的基本胜利。",
            "per_capita_consumption": 0,
            "business_hours": "05:00-22:00",
            "event_id": 5
        },
        
        # 6. 建国后时期 (1949-至今)
        {
            "name": "抗美援朝纪念馆",
            "address": "辽宁省丹东市振兴区山上街7号",
            "longitude": 124.3835,
            "latitude": 40.1258,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念抗美援朝战争的专题纪念馆",
            "historical_background": "1950-1953年，中国人民志愿军赴朝作战，与朝鲜军民并肩战斗，打败了以美国为首的联合国军，保卫了新中国的安全。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:30（周一闭馆）",
            "event_id": 6
        },
        {
            "name": "雷锋纪念馆",
            "address": "辽宁省抚顺市望花区雷锋路东段61号",
            "longitude": 123.9115,
            "latitude": 41.8473,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念共产主义战士雷锋的专题纪念馆",
            "historical_background": "雷锋是新中国成立后涌现出的优秀共产主义战士，全心全意为人民服务的光辉典范，1962年因公殉职，年仅22岁。",
            "per_capita_consumption": 0,
            "business_hours": "09:00-16:00（周一闭馆）",
            "event_id": 6
        },
        {
            "name": "大庆油田历史陈列馆",
            "address": "黑龙江省大庆市萨尔图区中七路",
            "longitude": 125.1028,
            "latitude": 46.5892,
            "category": AttractionCategory.博物馆.value,
            "brief_intro": "展示大庆油田开发建设历史的专题馆",
            "historical_background": "1959年发现大庆油田，1960年开展石油大会战，铁人王进喜等老一辈石油工人铸就了'大庆精神'和'铁人精神'。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:00",
            "event_id": 6
        },
        {
            "name": "铁人王进喜纪念馆",
            "address": "黑龙江省大庆市让胡路区中原路2号",
            "longitude": 124.8563,
            "latitude": 46.6524,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念'铁人'王进喜的专题纪念馆",
            "historical_background": "王进喜是大庆石油工人的杰出代表，'铁人精神'的化身，为我国石油工业发展作出了卓越贡献。",
            "per_capita_consumption": 0,
            "business_hours": "08:30-17:00（周一闭馆）",
            "event_id": 6
        },
        {
            "name": "两弹一星纪念馆",
            "address": "北京市海淀区怀柔区雁栖镇",
            "longitude": 116.6573,
            "latitude": 40.3616,
            "category": AttractionCategory.纪念馆.value,
            "brief_intro": "纪念'两弹一星'伟大成就的专题纪念馆",
            "historical_background": "1960-1970年代，我国在极端困难的条件下成功研制原子弹、氢弹和人造卫星，极大提升了国际地位和国防实力。",
            "per_capita_consumption": 30,
            "business_hours": "09:00-16:00（需预约）",
            "event_id": 6
        },
        # 四川省的两个"两弹一星"相关景点
        {
            "name": "两弹城景区",
            "address": "四川省绵阳市梓潼县文昌镇", 
            "longitude": 105.2278,
            "latitude": 31.6472,
            "category": AttractionCategory.其他纪念地.value,
            "brief_intro": "中国工程物理研究院旧址，'两弹一星'精神发源地",
            "historical_background": "1960年代，邓稼先等科学家在此研制核武器，成功研制出中国第一颗原子弹和氢弹，铸就了伟大的'两弹一星'精神。",
            "per_capita_consumption": 4000,  # 门票约40元
            "business_hours": "09:00-17:00",
            "event_id": 6
        },
        # 继续添加剩余景点...
        # 为节省篇幅，这里省略部分景点数据，实际应包含100个以上
    ]
    
    # 插入景点数据并建立事件关联
    for attraction in attractions:
        event_id = attraction.pop('event_id', None)
        
        # 插入景点
        cursor.execute('''
            INSERT INTO attractions (
                name, address, longitude, latitude, category, brief_intro, 
                historical_background, per_capita_consumption, business_hours
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (
            attraction['name'],
            attraction['address'],
            attraction['longitude'],
            attraction['latitude'],
            attraction['category'],
            attraction['brief_intro'],
            attraction['historical_background'],
            attraction['per_capita_consumption'],
            attraction['business_hours']
        ))
        
        # 获取刚插入的景点ID
        attraction_id = cursor.lastrowid
        
        # 如果有关联事件，插入关联记录
        if event_id:
            cursor.execute('''
                INSERT INTO attraction_events (attraction_id, event_id)
                VALUES (?, ?)
            ''', (attraction_id, event_id))
    
    conn.commit()
    conn.close()
    print("景点数据插入成功")

# 查询函数
def query_attractions_by_period(db_path: str = "red_tourism.db", period: str = None):
    """
    按历史时期查询景点
    """
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    if period:
        cursor.execute('''
            SELECT a.*, he.event_name, he.period
            FROM attractions a
            JOIN attraction_events ae ON a.id = ae.attraction_id
            JOIN historical_events he ON ae.event_id = he.id
            WHERE he.period = ?
            ORDER BY he.start_year
        ''', (period,))
    else:
        cursor.execute('''
            SELECT a.*, he.event_name, he.period
            FROM attractions a
            JOIN attraction_events ae ON a.id = ae.attraction_id
            JOIN historical_events he ON ae.event_id = he.id
            ORDER BY he.start_year, a.name
        ''')
    
    results = cursor.fetchall()
    conn.close()
    
    # 转换为更友好的格式
    columns = ['id', 'name', 'address', 'longitude', 'latitude', 'category', 
               'brief_intro', 'historical_background', 'per_capita_consumption', 
               'business_hours', 'created_at', 'updated_at', 'event_name', 'period']
    
    return [dict(zip(columns, row)) for row in results]

# 景点类型映射函数
def get_category_name(category_id: int) -> str:
    """根据category ID获取类型名称"""
    category_map = {c.value: c.name for c in AttractionCategory}
    return category_map.get(category_id, "未知")

# 主函数
def main():
    # 初始化数据库
    init_database()
    
    # 插入历史事件
    insert_historical_events()
    
    # 插入景点数据（实际应包含100个以上）
    insert_attractions()
    
    # 查询示例
    print("\n=== 按历史时期查询景点示例 ===")
    
    # 查询长征时期的景点
    print("\n--- 土地革命战争时期景点 ---")
    attractions = query_attractions_by_period(period="土地革命战争时期")
    for attr in attractions[:3]:  # 只显示前3个
        print(f"名称: {attr['name']}")
        print(f"地址: {attr['address']}")
        print(f"类型: {get_category_name(attr['category'])}")
        print(f"人均消费: {attr['per_capita_consumption']/100:.2f}元")
        print("-" * 50)
    
    # 统计信息
    conn = sqlite3.connect("red_tourism.db")
    cursor = conn.cursor()
    
    cursor.execute("SELECT COUNT(*) FROM attractions")
    total = cursor.fetchone()[0]
    
    cursor.execute("SELECT period, COUNT(*) FROM historical_events he JOIN attraction_events ae ON he.id = ae.event_id GROUP BY period")
    stats = cursor.fetchall()
    
    conn.close()
    
    print(f"\n数据库构建完成！")
    print(f"景点总数: {total}个")
    print("\n各历史时期景点分布:")
    for period, count in stats:
        print(f"  {period}: {count}个")

if __name__ == "__main__":
    main()