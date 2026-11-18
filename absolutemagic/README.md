# Absolute Magic (v0.3.0) - 未コンパイル・未検証

Iron's Spells 'n Spellbooks 3.16.2 (Forge 1.20.1) の追加スペル。バフ魔法を2つ追加する。
バフ中、攻撃が命中するたびに「追加の確定ダメージ」を1回与える。

| 魔法 | 内部名 | 学派 | 追加ダメージ |
|---|---|---|---|
| フィックスト・ストライク | fixed_damage | Holy | 固定値(初期: 3 × 魔法レベル。Lv5で15) |
| パーセント・ストライク | percent_damage | Ender | 敵HPの割合(初期: 0.04% × 魔法レベル。Lv5で0.2%、基準は最大HP) |

- 発動条件(攻撃時): ツール・武器の近接攻撃 / Iron's Spells の魔法ダメージ(SpellDamageSource)。
  飛び道具(矢など)は初期設定でオフ(`triggerProjectiles`)。
- 追加ダメージは「元の攻撃の威力」に依存しない。エンチャント・遺物(L2 Artifacts)・スキルツリー・魔力(スペルパワー)の影響を受けない。
  追加分は魔法ダメージ扱いではなく専用ダメージ種 `absolutemagic:transcend`。
  他modが追加分を増減しても、LivingHurtEvent / LivingDamageEvent の最低優先度で量を上書きして固定する。
- 判定は LivingDamageEvent の MONITOR(すべての計算の後・実際にダメージが通った時のみ)。
- 追加分はサーバーティックの終わりに与える。防具・耐性・盾・エンチャント軽減は無視(bypasses_* タグ)。
- 同じ攻撃者→同じ敵への追加ダメージは、初期設定で20tickに1回まで(`internalCooldownTicks`)。
  持続・多段ヒットの魔法で毎tick上乗せされるのを防ぐ。
- とどめは術者の撃破扱い(経験値・ドロップが入る)。味方・自分・プレイヤー(初期設定)には付かない。
- マナ 80 + 15/レベル、クールダウン 45秒、効果時間 30秒。2つは同時に使える。

## 設定 config/absolutemagic-common.toml
- [fixed_damage] damagePerLevel(3.0) / durationSeconds(30)
- [percent_damage] percentPerLevel(0.0004) / basis(MAX or CURRENT) / maxDamagePerHit(0=無制限) / durationSeconds(30)
- [common] triggerMelee / triggerProjectiles / triggerSpells / internalCooldownTicks(20) / affectPlayers

## ビルド
1. JDK 17。
2. libs/ に irons_spellbooks-1.20.1-3.16.2.jar を元のファイル名で置く(要求されたら irons_lib も)。
3. Forge 1.20.1-47.4.0 の MDK から gradlew と gradle/ をコピーする。
4. `./gradlew build` → build/libs/absolutemagic-0.3.0.jar を mods へ。

## 動作確認(魔法を覚える前)
- `/effect give @s absolutemagic:fixed_damage 60 4 true`(amplifier 4 = Lv5)
- `/effect give @s absolutemagic:percent_damage 60 4 true`

## v0.2 からの変更
- 魔法を「割合(元ダメージの1〜5%)」型から、固定値型・敵HP割合型に変更。内部名 weapon_transcend / magic_transcend は削除(v0.2 の jar とは併用しない)。
- 追加ダメージが元ダメージに依存しなくなり、他modによる増減を上書きして固定する処理を追加。
- 同一対象への連続発動を防ぐ内部クールダウンを追加。

## 未検証の点
- コンパイル・実機動作は未確認(Iron's の jar とのAPI照合と、javac の構文検査のみ)。
- L2Hostility の特性(適応など)が追加分を無効化する経路が、LivingAttackEvent での取り消しの場合は防げない。
- 吸収ハート(黄色ハート)は追加分でも先に削られる。
